package com.medcare.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.medcare.scheduling.OptimisticSchedulingService.RescheduleAppointmentCommand;
import com.medcare.scheduling.OptimisticSchedulingService.ScheduledAppointmentResponse;
import com.medcare.scheduling.OptimisticSchedulingService.SendOtpCommand;
import com.medcare.scheduling.OptimisticSchedulingService.SendOtpResponse;
import com.medcare.scheduling.OptimisticSchedulingService.VerifyOtpCommand;
import com.medcare.scheduling.OptimisticSchedulingService.VerifyOtpResponse;
import com.medcare.shared.exception.DomainException;
import com.medcare.tenancy.TenantContext;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
 * [AHC-45, AHC-46, AHC-47] Unit tests for:
 * - Clinic flow state machine transitions and audit timestamps (AHC-46)
 * - Drag-and-drop appointment rescheduling and conflict detection (AHC-45)
 * - Public booking OTP generation, validation, and token issuance (AHC-47)
 */
@ExtendWith(MockitoExtension.class)
class AppointmentClinicFlowUnitTest {

    private RedisSlotHoldStoreAdapter slotHoldStore;
    private TurnstileVerificationAdapter antiBotVerification;

    @Mock
    private ScheduledAppointmentRepository appointmentRepository;

    private OptimisticSchedulingService schedulingService;

    @BeforeEach
    void setUp() {
        TenantContext.setCurrentTenant("clinica-irma");
        slotHoldStore = new RedisSlotHoldStoreAdapter();
        antiBotVerification = new TurnstileVerificationAdapter();
        schedulingService = new OptimisticSchedulingService(
                slotHoldStore,
                antiBotVerification,
                appointmentRepository
        );
        lenient().when(appointmentRepository.saveAndFlush(any(ScheduledAppointmentEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        slotHoldStore.resetAll();
        TenantContext.clear();
    }

    // =========================================================================
    // [AHC-46] Clinic Flow State Machine Transitions & Audit Timestamps
    // =========================================================================

    @Test
    @DisplayName("[AHC-46 AC-46.1] Lobby check-in transitions SCHEDULED to WAITING_IN_LOBBY and records checkedInAt")
    void shouldTransitionScheduledToWaitingInLobbyAndRecordCheckedInAt() {
        UUID appointmentId = UUID.randomUUID();
        ScheduledAppointmentEntity entity = new ScheduledAppointmentEntity(
                appointmentId,
                "clinica-irma",
                "sede-norte",
                "room-1",
                "dr-alejandro-vega",
                "pat-1",
                "María Pérez",
                "+52 55 1111 2222",
                "Consulta General",
                "General",
                "hold-1",
                Instant.parse("2026-11-20T10:00:00Z"),
                Instant.parse("2026-11-20T10:30:00Z"),
                "SCHEDULED",
                null,
                null,
                null
        );

        when(appointmentRepository.findByIdAndTenantSlug(appointmentId, "clinica-irma"))
                .thenReturn(Optional.of(entity));

        ScheduledAppointmentResponse result = schedulingService.checkInPatient(appointmentId);

        assertThat(result.status()).isEqualTo("WAITING_IN_LOBBY");
        assertThat(result.checkedInAt()).isNotNull();
        assertThat(result.consultationStartedAt()).isNull();
        assertThat(result.waitingTimeMinutes()).isNotNull();
    }

    @Test
    @DisplayName("[AHC-46 AC-46.2] Starting consultation transitions WAITING_IN_LOBBY to IN_CONSULTATION and records consultationStartedAt")
    void shouldTransitionWaitingInLobbyToInConsultationAndRecordConsultationStartedAt() {
        UUID appointmentId = UUID.randomUUID();
        Instant checkedInAt = Instant.now().minusSeconds(600); // 10 minutes ago
        ScheduledAppointmentEntity entity = new ScheduledAppointmentEntity(
                appointmentId,
                "clinica-irma",
                "sede-norte",
                "room-1",
                "dr-alejandro-vega",
                "pat-1",
                "María Pérez",
                "+52 55 1111 2222",
                "Consulta General",
                "General",
                "hold-1",
                Instant.parse("2026-11-20T10:00:00Z"),
                Instant.parse("2026-11-20T10:30:00Z"),
                "WAITING_IN_LOBBY",
                checkedInAt,
                null,
                null
        );

        when(appointmentRepository.findByIdAndTenantSlug(appointmentId, "clinica-irma"))
                .thenReturn(Optional.of(entity));

        ScheduledAppointmentResponse result = schedulingService.startConsultation(appointmentId);

        assertThat(result.status()).isEqualTo("IN_CONSULTATION");
        assertThat(result.consultationStartedAt()).isNotNull();
        assertThat(result.waitingTimeMinutes()).isGreaterThanOrEqualTo(10L);
    }

    @Test
    @DisplayName("[AHC-46 AC-46.3] Completing consultation transitions IN_CONSULTATION to COMPLETED and records consultationCompletedAt")
    void shouldTransitionInConsultationToCompletedAndRecordConsultationCompletedAt() {
        UUID appointmentId = UUID.randomUUID();
        Instant checkedInAt = Instant.now().minusSeconds(1800); // 30 min ago
        Instant startedAt = Instant.now().minusSeconds(900); // 15 min ago
        ScheduledAppointmentEntity entity = new ScheduledAppointmentEntity(
                appointmentId,
                "clinica-irma",
                "sede-norte",
                "room-1",
                "dr-alejandro-vega",
                "pat-1",
                "María Pérez",
                "+52 55 1111 2222",
                "Consulta General",
                "General",
                "hold-1",
                Instant.parse("2026-11-20T10:00:00Z"),
                Instant.parse("2026-11-20T10:30:00Z"),
                "IN_CONSULTATION",
                checkedInAt,
                startedAt,
                null
        );

        when(appointmentRepository.findByIdAndTenantSlug(appointmentId, "clinica-irma"))
                .thenReturn(Optional.of(entity));

        ScheduledAppointmentResponse result = schedulingService.completeConsultation(appointmentId);

        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.consultationCompletedAt()).isNotNull();
        assertThat(result.consultationDurationMinutes()).isGreaterThanOrEqualTo(15L);
    }

    @Test
    @DisplayName("[AHC-46] Reject illegal state transitions: SCHEDULED directly to COMPLETED returns 400 Bad Request")
    void shouldRejectIllegalTransitionFromScheduledToCompleted() {
        UUID appointmentId = UUID.randomUUID();
        ScheduledAppointmentEntity entity = new ScheduledAppointmentEntity(
                appointmentId,
                "clinica-irma",
                "sede-norte",
                "room-1",
                "dr-alejandro-vega",
                "pat-1",
                "María Pérez",
                "+52 55 1111 2222",
                "Consulta General",
                "General",
                "hold-1",
                Instant.parse("2026-11-20T10:00:00Z"),
                Instant.parse("2026-11-20T10:30:00Z"),
                "SCHEDULED",
                null,
                null,
                null
        );

        when(appointmentRepository.findByIdAndTenantSlug(appointmentId, "clinica-irma"))
                .thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> schedulingService.transitionAppointmentStatus(appointmentId, "COMPLETED"))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException de = (DomainException) ex;
                    assertThat(de.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(de.getMessage()).contains("Cannot transition appointment from SCHEDULED directly to COMPLETED");
                });
    }

    @Test
    @DisplayName("[AHC-46] Reject transitions from COMPLETED to any other status")
    void shouldRejectTransitionFromCompletedStatus() {
        UUID appointmentId = UUID.randomUUID();
        ScheduledAppointmentEntity entity = new ScheduledAppointmentEntity(
                appointmentId,
                "clinica-irma",
                "sede-norte",
                "room-1",
                "dr-alejandro-vega",
                "pat-1",
                "María Pérez",
                "+52 55 1111 2222",
                "Consulta General",
                "General",
                "hold-1",
                Instant.parse("2026-11-20T10:00:00Z"),
                Instant.parse("2026-11-20T10:30:00Z"),
                "COMPLETED",
                Instant.now().minusSeconds(1000),
                Instant.now().minusSeconds(500),
                Instant.now()
        );

        when(appointmentRepository.findByIdAndTenantSlug(appointmentId, "clinica-irma"))
                .thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> schedulingService.transitionAppointmentStatus(appointmentId, "WAITING_IN_LOBBY"))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    // =========================================================================
    // [AHC-45] Rescheduling & Conflict Detection
    // =========================================================================

    @Test
    @DisplayName("[AHC-45 AC-3.1.3] Rescheduling an existing SCHEDULED appointment updates slot times atomically when slot is free")
    void shouldRescheduleAppointmentSuccessfullyWhenSlotIsFree() {
        UUID appointmentId = UUID.randomUUID();
        Instant originalStart = Instant.parse("2026-11-20T09:00:00Z");
        Instant originalEnd = Instant.parse("2026-11-20T09:30:00Z");
        Instant newStart = Instant.parse("2026-11-20T11:00:00Z");
        Instant newEnd = Instant.parse("2026-11-20T11:30:00Z");

        ScheduledAppointmentEntity entity = new ScheduledAppointmentEntity(
                appointmentId,
                "clinica-irma",
                "sede-norte",
                "room-1",
                "dr-alejandro-vega",
                "pat-1",
                "Carlos Gómez",
                "+52 55 3333 4444",
                "Consulta General",
                "General",
                "hold-1",
                originalStart,
                originalEnd,
                "SCHEDULED",
                null,
                null,
                null
        );

        when(appointmentRepository.findByIdAndTenantSlug(appointmentId, "clinica-irma"))
                .thenReturn(Optional.of(entity));
        when(appointmentRepository.findOverlappingByDoctorExcludingId(
                eq("clinica-irma"), eq("dr-alejandro-vega"), eq(appointmentId), eq(newStart), eq(newEnd)
        )).thenReturn(List.of());
        when(appointmentRepository.findOverlappingByRoomExcludingId(
                eq("clinica-irma"), eq("room-1"), eq(appointmentId), eq(newStart), eq(newEnd)
        )).thenReturn(List.of());

        ScheduledAppointmentResponse rescheduled = schedulingService.rescheduleAppointment(
                appointmentId,
                new RescheduleAppointmentCommand(newStart, newEnd, null, null)
        );

        assertThat(rescheduled.startTime()).isEqualTo(newStart);
        assertThat(rescheduled.endTime()).isEqualTo(newEnd);
        assertThat(rescheduled.status()).isEqualTo("SCHEDULED");
    }

    @Test
    @DisplayName("[AHC-45 AC-3.1.3] Rescheduling rejects doctor double-booking collision with 409 Conflict")
    void shouldRejectRescheduleWith409ConflictWhenDoctorHasOverlap() {
        UUID appointmentId = UUID.randomUUID();
        Instant newStart = Instant.parse("2026-11-20T11:00:00Z");
        Instant newEnd = Instant.parse("2026-11-20T11:30:00Z");

        ScheduledAppointmentEntity entity = new ScheduledAppointmentEntity(
                appointmentId,
                "clinica-irma",
                "sede-norte",
                "room-1",
                "dr-alejandro-vega",
                "pat-1",
                "Carlos Gómez",
                "+52 55 3333 4444",
                "Consulta General",
                "General",
                "hold-1",
                Instant.parse("2026-11-20T09:00:00Z"),
                Instant.parse("2026-11-20T09:30:00Z"),
                "SCHEDULED",
                null,
                null,
                null
        );

        ScheduledAppointmentEntity conflicting = new ScheduledAppointmentEntity(
                UUID.randomUUID(),
                "clinica-irma",
                "sede-norte",
                "room-2",
                "dr-alejandro-vega",
                "pat-2",
                "Elena Peña",
                "+52 55 5555 6666",
                "Consulta General",
                "General",
                "hold-2",
                newStart,
                newEnd,
                "SCHEDULED",
                null,
                null,
                null
        );

        when(appointmentRepository.findByIdAndTenantSlug(appointmentId, "clinica-irma"))
                .thenReturn(Optional.of(entity));
        when(appointmentRepository.findOverlappingByDoctorExcludingId(
                eq("clinica-irma"), eq("dr-alejandro-vega"), eq(appointmentId), eq(newStart), eq(newEnd)
        )).thenReturn(List.of(conflicting));

        assertThatThrownBy(() -> schedulingService.rescheduleAppointment(
                appointmentId,
                new RescheduleAppointmentCommand(newStart, newEnd, null, null)
        ))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    // =========================================================================
    // [AHC-47] OTP Verification & Public Self-Service
    // =========================================================================

    @Test
    @DisplayName("[AHC-47 AC-47.2 & AC-47.3] Dispatch OTP, verify 6-digit code and generate patient Bearer JWT")
    void shouldDispatchOtpAndVerifySuccessfullyWithBearerToken() {
        SendOtpResponse sendResponse = schedulingService.sendOtp(new SendOtpCommand(
                "+52 55 9988 7766",
                "Lucía Morales"
        ));

        assertThat(sendResponse.status()).isEqualTo("SENT");
        assertThat(sendResponse.expiresInSeconds()).isEqualTo(300);

        // Verify with valid code (123456)
        VerifyOtpResponse verifyResponse = schedulingService.verifyOtp(new VerifyOtpCommand(
                "+52 55 9988 7766",
                "123456",
                "Lucía Morales"
        ));

        assertThat(verifyResponse.token()).startsWith("Bearer eyJ");
        assertThat(verifyResponse.patientId()).startsWith("pat-");
        assertThat(verifyResponse.patientName()).isEqualTo("Lucía Morales");
    }

    @Test
    @DisplayName("[AHC-47 AC-47.3] Reject invalid OTP code with 401 Unauthorized")
    void shouldRejectInvalidOtpCodeWith401Unauthorized() {
        schedulingService.sendOtp(new SendOtpCommand("+52 55 9988 7766", "Lucía Morales"));

        assertThatThrownBy(() -> schedulingService.verifyOtp(new VerifyOtpCommand(
                "+52 55 9988 7766",
                "999999",
                "Lucía Morales"
        )))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }
}
