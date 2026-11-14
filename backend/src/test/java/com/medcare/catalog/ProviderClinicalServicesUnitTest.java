package com.medcare.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.medcare.catalog.ProviderServiceCatalogService.BookAppointmentCommand;
import com.medcare.catalog.ProviderServiceCatalogService.ClinicalServiceResponse;
import com.medcare.catalog.ProviderServiceCatalogService.CreateServiceCommand;
import com.medcare.catalog.ProviderServiceCatalogService.ServiceAppointmentResponse;
import com.medcare.shared.exception.DomainException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

/**
 * [AHC-24] JUnit 5 Unit Tests for Story AHC-5:
 * Provider Clinical Services & Pricing Catalog.
 */
@ExtendWith(MockitoExtension.class)
class ProviderClinicalServicesUnitTest {

    @Mock
    private ClinicalServiceRepository serviceRepository;

    @Mock
    private ServiceAppointmentRepository appointmentRepository;

    private ProviderServiceCatalogService catalogService;

    @BeforeEach
    void setUp() {
        catalogService = new ProviderServiceCatalogService(serviceRepository, appointmentRepository);
    }

    @Test
    @DisplayName("AC-1: Should create clinical service 'First-Time Cardiology Evaluation' (45 min, $800 MXN) under provider specialty")
    void shouldCreateClinicalServiceUnderProviderProfileAndSpecialty() {
        String providerId = UUID.randomUUID().toString();
        var command = new CreateServiceCommand(
                "clinica-irma",
                providerId,
                "First-Time Cardiology Evaluation",
                "Cardiology",
                45,
                new BigDecimal("800.00"),
                "MXN"
        );
        when(serviceRepository.save(any(ClinicalServiceEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        ClinicalServiceResponse created = catalogService.createService(command);

        assertThat(created).isNotNull();
        assertThat(created.providerId()).isEqualTo(providerId);
        assertThat(created.specialty()).isEqualTo("Cardiology");
        assertThat(created.name()).isEqualTo("First-Time Cardiology Evaluation");
        assertThat(created.durationMinutes()).isEqualTo(45);
        assertThat(created.feeAmount()).isEqualByComparingTo("800.00");
        assertThat(created.currency()).isEqualTo("MXN");
        assertThat(created.status()).isEqualTo("ACTIVE");
        verify(serviceRepository).save(any(ClinicalServiceEntity.class));
    }

    @Test
    @DisplayName("AC-1: Should reject clinical service creation when duration or consultation fee is non-positive")
    void shouldRejectServiceWithInvalidDurationOrNegativeFee() {
        String providerId = UUID.randomUUID().toString();
        var invalidFeeCommand = new CreateServiceCommand(
                "clinica-irma",
                providerId,
                "Free Invalid Service",
                "Cardiology",
                45,
                new BigDecimal("-50.00"),
                "MXN"
        );

        assertThatThrownBy(() -> catalogService.createService(invalidFeeCommand))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException domainEx = (DomainException) ex;
                    assertThat(domainEx.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(domainEx.getMessage()).contains("Consultation fee must be strictly positive");
                });

        verify(serviceRepository, never()).save(any());
    }

    @Test
    @DisplayName("AC-2: Selecting 'Minor Procedure' (60 min) at 10:00 AM should allocate continuous block 10:00-11:00 AM and reject partial overlaps")
    void shouldAllocateContinuous60MinuteBlockAndRejectOverlappingCheckup() {
        String providerId = UUID.randomUUID().toString();
        UUID minorProcedureId = UUID.randomUUID();
        UUID routineCheckupId = UUID.randomUUID();

        var minorProcedure = new ClinicalServiceEntity(
                minorProcedureId, "clinica-irma", providerId, "General Surgery",
                "Minor Procedure", 60, new BigDecimal("1500.00"), "MXN", "ACTIVE"
        );
        var routineCheckup = new ClinicalServiceEntity(
                routineCheckupId, "clinica-irma", providerId, "General Surgery",
                "Routine Checkup", 20, new BigDecimal("500.00"), "MXN", "ACTIVE"
        );

        Instant tenAm = Instant.parse("2026-11-16T10:00:00Z");
        Instant elevenAm = Instant.parse("2026-11-16T11:00:00Z");

        when(serviceRepository.findById(minorProcedureId)).thenReturn(Optional.of(minorProcedure));
        when(appointmentRepository.existsOverlappingAppointment(providerId, tenAm, elevenAm)).thenReturn(false);
        when(appointmentRepository.save(any(ServiceAppointmentEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        ServiceAppointmentResponse bookedBlock = catalogService.bookServiceAppointment(
                new BookAppointmentCommand("clinica-irma", providerId, minorProcedureId, "Carlos Rivera", tenAm)
        );

        assertThat(bookedBlock.startTime()).isEqualTo(tenAm);
        assertThat(bookedBlock.endTime()).isEqualTo(elevenAm);
        assertThat(bookedBlock.billedFee()).isEqualByComparingTo("1500.00");

        Instant tenThirtyAm = Instant.parse("2026-11-16T10:30:00Z");
        Instant tenFiftyAm = Instant.parse("2026-11-16T10:50:00Z");
        when(serviceRepository.findById(routineCheckupId)).thenReturn(Optional.of(routineCheckup));
        when(appointmentRepository.existsOverlappingAppointment(providerId, tenThirtyAm, tenFiftyAm)).thenReturn(true);

        assertThatThrownBy(() -> catalogService.bookServiceAppointment(
                new BookAppointmentCommand("clinica-irma", providerId, routineCheckupId, "Ana Gómez", tenThirtyAm)
        ))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException domainEx = (DomainException) ex;
                    assertThat(domainEx.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(domainEx.getMessage()).contains("Overlapping appointment block");
                });
    }

    @Test
    @DisplayName("AC-3: Marking service INACTIVE should hide it from active booking flows while preserving historical appointment and billing records")
    void shouldSoftDeactivateServiceAndPreserveHistoricalAppointmentIntegrity() {
        String providerId = UUID.randomUUID().toString();
        UUID serviceId = UUID.randomUUID();
        UUID historicalAppointmentId = UUID.randomUUID();

        var activeService = new ClinicalServiceEntity(
                serviceId, "clinica-irma", providerId, "Cardiology",
                "First-Time Cardiology Evaluation", 45, new BigDecimal("800.00"), "MXN", "ACTIVE"
        );
        var historicalAppointment = new ServiceAppointmentEntity(
                historicalAppointmentId,
                "clinica-irma",
                providerId,
                serviceId,
                "First-Time Cardiology Evaluation",
                "Elena Torres",
                Instant.parse("2026-10-10T09:00:00Z"),
                Instant.parse("2026-10-10T09:45:00Z"),
                45,
                new BigDecimal("800.00"),
                "COMPLETED"
        );

        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(activeService));
        when(serviceRepository.save(any(ClinicalServiceEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(appointmentRepository.findById(historicalAppointmentId)).thenReturn(Optional.of(historicalAppointment));

        ClinicalServiceResponse deactivated = catalogService.updateServiceStatus(providerId, serviceId, "INACTIVE");

        assertThat(deactivated.status()).isEqualTo("INACTIVE");

        assertThatThrownBy(() -> catalogService.bookServiceAppointment(
                new BookAppointmentCommand(
                        "clinica-irma", providerId, serviceId, "Paciente Nuevo",
                        Instant.parse("2026-11-20T12:00:00Z")
                )
        ))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("INACTIVE");

        ServiceAppointmentResponse preservedHistory = catalogService.getHistoricalAppointment(historicalAppointmentId);
        assertThat(preservedHistory.serviceId()).isEqualTo(serviceId);
        assertThat(preservedHistory.serviceNameSnapshot()).isEqualTo("First-Time Cardiology Evaluation");
        assertThat(preservedHistory.billedFee()).isEqualByComparingTo("800.00");
        assertThat(preservedHistory.status()).isEqualTo("COMPLETED");
    }
}
