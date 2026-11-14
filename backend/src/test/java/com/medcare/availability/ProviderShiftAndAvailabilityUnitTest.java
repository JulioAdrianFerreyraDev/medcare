package com.medcare.availability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.medcare.availability.ProviderAvailabilityService.BookableSlotResponse;
import com.medcare.availability.ProviderAvailabilityService.ConfigureWeeklyShiftTemplateCommand;
import com.medcare.availability.ProviderAvailabilityService.ProviderShiftResponse;
import com.medcare.availability.ProviderAvailabilityService.ShiftWindowInput;
import com.medcare.scheduling.ScheduledAppointmentRepository;
import com.medcare.scheduling.SlotHoldStorePort;
import com.medcare.shared.exception.DomainException;
import com.medcare.tenancy.TenantContext;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

/**
 * [AHC-29 / Story AHC-6] Unit Tests for Provider Working Hours, Meal Break Blocking & Cross-Branch Shift Collision.
 */
@ExtendWith(MockitoExtension.class)
class ProviderShiftAndAvailabilityUnitTest {

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
        lenient().when(shiftRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("[AHC-6 AC-1] Weekly shift configuration on Tuesdays (09:00-13:00 & 15:00-19:00) exposes slots strictly inside windows and keeps 13:00-15:00 blocked")
    void shouldExposeBookableIntervalsOnlyWithinShiftWindowsAndKeepMealBreakBlocked() {
        String providerId = "dr-vega";
        String branchId = "sede-norte";

        ConfigureWeeklyShiftTemplateCommand command = new ConfigureWeeklyShiftTemplateCommand(
                branchId,
                "Sede Norte",
                "TUESDAY",
                60,
                List.of(
                        new ShiftWindowInput(LocalTime.of(9, 0), LocalTime.of(13, 0)),
                        new ShiftWindowInput(LocalTime.of(15, 0), LocalTime.of(19, 0))
                )
        );

        when(shiftRepository.findAllByTenantSlugAndProviderIdAndDayOfWeek("clinica-irma", providerId, "TUESDAY"))
                .thenReturn(List.of());

        List<ProviderShiftResponse> savedShifts = availabilityService.configureWeeklyShifts(providerId, command);
        assertThat(savedShifts).hasSize(2);

        List<ProviderShiftEntity> persistedEntities = List.of(
                new ProviderShiftEntity(
                        UUID.randomUUID(),
                        "clinica-irma",
                        providerId,
                        branchId,
                        "Sede Norte",
                        "TUESDAY",
                        LocalTime.of(9, 0),
                        LocalTime.of(13, 0),
                        60
                ),
                new ProviderShiftEntity(
                        UUID.randomUUID(),
                        "clinica-irma",
                        providerId,
                        branchId,
                        "Sede Norte",
                        "TUESDAY",
                        LocalTime.of(15, 0),
                        LocalTime.of(19, 0),
                        60
                )
        );

        // 2026-11-17 is a Tuesday
        LocalDate tuesday = LocalDate.of(2026, 11, 17);
        when(shiftRepository.findAllByTenantSlugAndProviderIdAndBranchIdAndDayOfWeek(
                "clinica-irma", providerId, branchId, "TUESDAY"
        )).thenReturn(persistedEntities);
        when(timeOffBlockRepository.findOverlappingBlocks(anyString(), anyString(), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());
        when(scheduledAppointmentRepository.findOverlappingScheduledByDoctor(anyString(), anyString(), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());
        when(slotHoldStore.isSlotHeld(anyString(), anyString(), any(Instant.class), any(Instant.class)))
                .thenReturn(false);

        List<BookableSlotResponse> slots = availabilityService.getAvailableSlots(providerId, branchId, tuesday, 60);

        // 09:00-13:00 yields 4 one-hour slots; 15:00-19:00 yields 4 one-hour slots = 8 total slots
        assertThat(slots).hasSize(8);
        assertThat(slots)
                .extracting(BookableSlotResponse::localStartTime)
                .containsExactly(
                        LocalTime.of(9, 0),
                        LocalTime.of(10, 0),
                        LocalTime.of(11, 0),
                        LocalTime.of(12, 0),
                        LocalTime.of(15, 0),
                        LocalTime.of(16, 0),
                        LocalTime.of(17, 0),
                        LocalTime.of(18, 0)
                )
                .doesNotContain(LocalTime.of(13, 0), LocalTime.of(14, 0));
    }

    @Test
    @DisplayName("[AHC-6 AC-2] Rejects cross-branch shift collision (Sede Norte 09:00-14:00 vs Sede Sur 12:00-17:00 on Tuesdays) with HTTP 422 Unprocessable Entity")
    void shouldRejectCrossBranchShiftCollisionWith422() {
        String providerId = "dr-vega";

        ProviderShiftEntity existingSedeNorteShift = new ProviderShiftEntity(
                UUID.randomUUID(),
                "clinica-irma",
                providerId,
                "sede-norte",
                "Sede Norte",
                "TUESDAY",
                LocalTime.of(9, 0),
                LocalTime.of(14, 0),
                30
        );

        when(shiftRepository.findAllByTenantSlugAndProviderIdAndDayOfWeek("clinica-irma", providerId, "TUESDAY"))
                .thenReturn(List.of(existingSedeNorteShift));

        ConfigureWeeklyShiftTemplateCommand conflictingSedeSurCommand = new ConfigureWeeklyShiftTemplateCommand(
                "sede-sur",
                "Sede Sur",
                "TUESDAY",
                30,
                List.of(new ShiftWindowInput(LocalTime.of(12, 0), LocalTime.of(17, 0)))
        );

        assertThatThrownBy(() -> availabilityService.configureWeeklyShifts(providerId, conflictingSedeSurCommand))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException domainEx = (DomainException) ex;
                    assertThat(domainEx.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(domainEx.getTitle()).isEqualTo("Cross-Branch Shift Conflict");
                    assertThat(domainEx.getMessage()).contains("Sede Norte", "Sede Sur", "12:00", "14:00");
                });
    }
}
