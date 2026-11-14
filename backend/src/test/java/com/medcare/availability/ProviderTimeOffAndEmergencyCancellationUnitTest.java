package com.medcare.availability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.medcare.availability.ProviderAvailabilityService.BookableSlotResponse;
import com.medcare.availability.ProviderAvailabilityService.EmergencyCancellationCommand;
import com.medcare.availability.ProviderAvailabilityService.PlannedLeaveCommand;
import com.medcare.availability.ProviderAvailabilityService.TimeOffBlockResponse;
import com.medcare.scheduling.ScheduledAppointmentEntity;
import com.medcare.scheduling.ScheduledAppointmentRepository;
import com.medcare.scheduling.SlotHoldStorePort;
import com.medcare.shared.exception.DomainException;
import com.medcare.tenancy.TenantContext;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

/**
 * [AHC-33 / Story AHC-7] Unit Tests for Planned Leave Blocking, Emergency Cancellation Workflow,
 * Receptionist Dashboard Alerts, Patient Notifications, and Immutable Audit Trail.
 */
@ExtendWith(MockitoExtension.class)
class ProviderTimeOffAndEmergencyCancellationUnitTest {

    @Mock
    private ProviderShiftRepository shiftRepository;

    @Mock
    private ProviderTimeOffBlockRepository timeOffBlockRepository;

    @Mock
    private ScheduleCancellationAuditLogRepository auditLogRepository;

    @Mock
    private ReceptionistDashboardAlertRepository receptionistAlertRepository;

    @Mock
    private ScheduledAppointmentRepository scheduledAppointmentRepository;

    @Mock
    private SlotHoldStorePort slotHoldStore;

    @Mock
    private PatientReschedulingNotificationPort notificationPort;

    private ProviderAvailabilityService availabilityService;

    @BeforeEach
    void setUp() {
        TenantContext.setCurrentTenant("clinica-irma");
        availabilityService = new ProviderAvailabilityService(
                shiftRepository,
                timeOffBlockRepository,
                auditLogRepository,
                receptionistAlertRepository,
                scheduledAppointmentRepository,
                slotHoldStore,
                notificationPort
        );
        lenient().when(timeOffBlockRepository.save(any(ProviderTimeOffBlockEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(auditLogRepository.save(any(ScheduleCancellationAuditLogEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(receptionistAlertRepository.save(any(ReceptionistDashboardAlertEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("[AHC-7 AC-1 & AC-3] Planned leave from Dec 20 to Dec 27 with zero appointments suppresses all slots in range and records immutable audit log")
    void shouldSuppressAllSlotsDuringPlannedLeaveAndRecordAuditLog() {
        String providerId = "dr-vega";
        String branchId = "sede-norte";
        Instant leaveStart = Instant.parse("2026-12-20T00:00:00Z");
        Instant leaveEnd = Instant.parse("2026-12-27T23:59:59Z");

        when(scheduledAppointmentRepository.findOverlappingScheduledByDoctor(
                "clinica-irma", providerId, leaveStart, leaveEnd
        )).thenReturn(List.of());

        TimeOffBlockResponse response = availabilityService.registerPlannedLeave(
                providerId,
                new PlannedLeaveCommand(branchId, leaveStart, leaveEnd, "Annual December Holiday Leave")
        );

        assertThat(response.blockType()).isEqualTo("PLANNED_LEAVE");
        assertThat(response.affectedAppointmentsCount()).isZero();
        assertThat(response.auditLogId()).isNotNull();

        // Verify slot generation on Tuesday Dec 22, 2026 is completely suppressed by the planned leave block
        LocalDate dec22Tuesday = LocalDate.of(2026, 12, 22);
        when(shiftRepository.findAllByTenantSlugAndProviderIdAndBranchIdAndDayOfWeek(
                "clinica-irma", providerId, branchId, "TUESDAY"
        )).thenReturn(List.of(new ProviderShiftEntity(
                UUID.randomUUID(),
                "clinica-irma",
                providerId,
                branchId,
                "Sede Norte",
                "TUESDAY",
                LocalTime.of(9, 0),
                LocalTime.of(13, 0),
                60
        )));
        when(timeOffBlockRepository.findOverlappingBlocks(anyString(), eq(providerId), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(new ProviderTimeOffBlockEntity(
                        response.id(),
                        "clinica-irma",
                        providerId,
                        branchId,
                        "PLANNED_LEAVE",
                        leaveStart,
                        leaveEnd,
                        "Annual December Holiday Leave",
                        0
                )));

        List<BookableSlotResponse> slotsOnDec22 = availabilityService.getAvailableSlots(
                providerId, branchId, dec22Tuesday, 60
        );
        assertThat(slotsOnDec22).isEmpty();
    }

    @Test
    @DisplayName("[AHC-7 AC-2 & AC-3] Emergency cancellation with 5 scheduled appointments transitions all 5 to REQUIRES_RESCHEDULING, creates HIGH receptionist alert, notifies 5 patients, and writes audit log")
    void shouldTransitionFiveAppointmentsAndCreateReceptionistAlertAndAuditLogOnEmergencyCancellation() {
        String providerId = "dr-vega";
        String branchId = "sede-norte";
        Instant emergencyStart = Instant.parse("2026-11-18T08:00:00Z");
        Instant emergencyEnd = Instant.parse("2026-11-18T18:00:00Z");

        List<ScheduledAppointmentEntity> fiveAppointments = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            fiveAppointments.add(new ScheduledAppointmentEntity(
                    UUID.randomUUID(),
                    "clinica-irma",
                    branchId,
                    "room-1",
                    providerId,
                    "patient-" + i,
                    "Patient " + i,
                    "patient" + i + "@example.com",
                    "hold_token_" + i,
                    Instant.parse(String.format("2026-11-18T%02d:00:00Z", 8 + i)),
                    Instant.parse(String.format("2026-11-18T%02d:00:00Z", 9 + i)),
                    "SCHEDULED"
            ));
        }

        when(scheduledAppointmentRepository.findOverlappingScheduledByDoctor(
                "clinica-irma", providerId, emergencyStart, emergencyEnd
        )).thenReturn(fiveAppointments);

        TimeOffBlockResponse response = availabilityService.triggerEmergencyCancellation(
                providerId,
                new EmergencyCancellationCommand(
                        branchId,
                        emergencyStart,
                        emergencyEnd,
                        "Unforeseen inpatient cardiac surgery emergency"
                )
        );

        assertThat(response.blockType()).isEqualTo("EMERGENCY_CANCELLATION");
        assertThat(response.affectedAppointmentsCount()).isEqualTo(5);
        assertThat(response.rescheduledAppointmentIds()).hasSize(5);
        assertThat(response.receptionistAlertId()).isNotNull();
        assertThat(response.auditLogId()).isNotNull();

        // All 5 appointments must transition to REQUIRES_RESCHEDULING
        assertThat(fiveAppointments)
                .extracting(ScheduledAppointmentEntity::getStatus)
                .containsOnly("REQUIRES_RESCHEDULING");
        verify(scheduledAppointmentRepository).saveAll(fiveAppointments);

        // Automated notifications dispatched to all 5 patients
        verify(notificationPort, times(5)).notifyPatientOfEmergencyCancellation(
                eq("clinica-irma"),
                any(UUID.class),
                anyString(),
                anyString(),
                eq(providerId),
                eq("Unforeseen inpatient cardiac surgery emergency")
        );

        // High-priority receptionist alert created
        ArgumentCaptor<ReceptionistDashboardAlertEntity> alertCaptor =
                ArgumentCaptor.forClass(ReceptionistDashboardAlertEntity.class);
        verify(receptionistAlertRepository).save(alertCaptor.capture());
        assertThat(alertCaptor.getValue().getSeverity()).isEqualTo("HIGH");
        assertThat(alertCaptor.getValue().getAffectedAppointmentsCount()).isEqualTo(5);

        // Immutable audit trail entry recorded
        ArgumentCaptor<ScheduleCancellationAuditLogEntity> auditCaptor =
                ArgumentCaptor.forClass(ScheduleCancellationAuditLogEntity.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getProviderId()).isEqualTo(providerId);
        assertThat(auditCaptor.getValue().getReason()).isEqualTo("Unforeseen inpatient cardiac surgery emergency");
        assertThat(auditCaptor.getValue().getRecordedAt()).isNotNull();
    }

    @Test
    @DisplayName("[AHC-7 AC-2] Emergency cancellation without mandatory reason is rejected with HTTP 400 Bad Request")
    void shouldRejectEmergencyCancellationWithoutMandatoryReason() {
        assertThatThrownBy(() -> availabilityService.triggerEmergencyCancellation(
                "dr-vega",
                new EmergencyCancellationCommand(
                        "sede-norte",
                        Instant.parse("2026-11-18T08:00:00Z"),
                        Instant.parse("2026-11-18T12:00:00Z"),
                        "   "
                )
        ))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }
}
