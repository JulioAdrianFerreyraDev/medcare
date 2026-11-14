package com.medcare.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.medcare.scheduling.OptimisticSchedulingService.AcquireSlotHoldCommand;
import com.medcare.scheduling.OptimisticSchedulingService.ConfirmAppointmentCommand;
import com.medcare.scheduling.OptimisticSchedulingService.ScheduledAppointmentResponse;
import com.medcare.scheduling.OptimisticSchedulingService.SlotHoldResponse;
import com.medcare.shared.exception.DomainException;
import com.medcare.tenancy.TenantContext;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

/**
 * [AHC-38 / Story AHC-8] Unit Tests for Unauthenticated Optimistic Slot Hold (300s TTL, IP Rate Limit),
 * Authenticated Confirmation with Hold Ownership, Automatic Release on Timeout, and Atomic Exclusion.
 */
@ExtendWith(MockitoExtension.class)
class OptimisticSlotHoldAndBookingUnitTest {

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
        lenient().when(appointmentRepository.findOverlappingScheduledByDoctor(
                anyString(), anyString(), any(Instant.class), any(Instant.class)
        )).thenReturn(List.of());
        lenient().when(appointmentRepository.findOverlappingScheduledByRoom(
                anyString(), anyString(), any(Instant.class), any(Instant.class)
        )).thenReturn(List.of());
        lenient().when(appointmentRepository.saveAndFlush(any(ScheduledAppointmentEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        slotHoldStore.resetAll();
        TenantContext.clear();
    }

    @Test
    @DisplayName("[AHC-8 AC-1] Unauthenticated visitor passing Turnstile within IP rate limits creates lock:doctor:X:slot:1000 with 300s TTL and hides slot; 3rd hold from same IP is rejected with 429")
    void shouldCreateRedisSlotLockWith300sTtlAndEnforceMaxTwoActiveHoldsPerIp() {
        Instant slot1000Start = Instant.parse("2026-11-17T10:00:00Z");
        Instant slot1000End = Instant.parse("2026-11-17T10:30:00Z");

        SlotHoldResponse hold1 = schedulingService.acquireOptimisticSlotHold(
                new AcquireSlotHoldCommand("sede-norte", "X", slot1000Start, slot1000End, "turnstile-valid-token"),
                "203.0.113.42"
        );

        assertThat(hold1.redisKey()).isEqualTo("lock:doctor:X:slot:1000");
        assertThat(hold1.holdToken()).startsWith("hold_");
        assertThat(hold1.ttlSeconds()).isEqualTo(300L);
        assertThat(slotHoldStore.isSlotHeld("clinica-irma", "X", slot1000Start, slot1000End)).isTrue();

        // Another user attempting to hold the same 10:00 AM slot is blocked with 409 Conflict
        assertThatThrownBy(() -> schedulingService.acquireOptimisticSlotHold(
                new AcquireSlotHoldCommand("sede-norte", "X", slot1000Start, slot1000End, "turnstile-valid-token"),
                "198.51.100.77"
        ))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));

        // Same IP (203.0.113.42) can hold a 2nd slot (11:00 AM)
        SlotHoldResponse hold2 = schedulingService.acquireOptimisticSlotHold(
                new AcquireSlotHoldCommand(
                        "sede-norte",
                        "X",
                        Instant.parse("2026-11-17T11:00:00Z"),
                        Instant.parse("2026-11-17T11:30:00Z"),
                        "turnstile-valid-token"
                ),
                "203.0.113.42"
        );
        assertThat(hold2.redisKey()).isEqualTo("lock:doctor:X:slot:1100");

        // 3rd active hold from the same IP (203.0.113.42) is rejected with HTTP 429 Too Many Requests
        assertThatThrownBy(() -> schedulingService.acquireOptimisticSlotHold(
                new AcquireSlotHoldCommand(
                        "sede-norte",
                        "X",
                        Instant.parse("2026-11-17T12:00:00Z"),
                        Instant.parse("2026-11-17T12:30:00Z"),
                        "turnstile-valid-token"
                ),
                "203.0.113.42"
        ))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
    }

    @Test
    @DisplayName("[AHC-8 AC-2] Authenticated confirmation with valid JWT and matching holdToken persists SCHEDULED appointment and deletes Redis lock")
    void shouldConfirmAppointmentWithValidJwtAndMatchingHoldTokenAndDeleteRedisLock() {
        Instant slot1000Start = Instant.parse("2026-11-17T10:00:00Z");
        Instant slot1000End = Instant.parse("2026-11-17T10:30:00Z");

        SlotHoldResponse hold = schedulingService.acquireOptimisticSlotHold(
                new AcquireSlotHoldCommand("sede-norte", "X", slot1000Start, slot1000End, "turnstile-valid-token"),
                "203.0.113.50"
        );

        ScheduledAppointmentResponse confirmed = schedulingService.confirmAppointment(
                new ConfirmAppointmentCommand(
                        "sede-norte",
                        "room-cardio-1",
                        "X",
                        "patient-101",
                        "María Fernanda López",
                        "+52 55 4433 2211",
                        hold.holdToken(),
                        slot1000Start,
                        slot1000End
                ),
                "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.valid-otp-jwt"
        );

        assertThat(confirmed.status()).isEqualTo("SCHEDULED");
        assertThat(confirmed.holdToken()).isEqualTo(hold.holdToken());

        // Redis lock must be deleted after confirmation
        assertThat(slotHoldStore.getActiveHoldByToken(hold.holdToken())).isEmpty();
        assertThat(slotHoldStore.isSlotHeld("clinica-irma", "X", slot1000Start, slot1000End)).isFalse();
    }

    @Test
    @DisplayName("[AHC-8 AC-3] Automatic release on 300-second timeout evicts Redis lock and returns 10:00 AM slot to public availability")
    void shouldEvictSlotHoldAutomaticallyAfter300SecondsTimeout() {
        Instant slot1000Start = Instant.parse("2026-11-17T10:00:00Z");
        Instant slot1000End = Instant.parse("2026-11-17T10:30:00Z");

        SlotHoldResponse hold = schedulingService.acquireOptimisticSlotHold(
                new AcquireSlotHoldCommand("sede-norte", "X", slot1000Start, slot1000End, "turnstile-valid-token"),
                "203.0.113.60"
        );

        assertThat(slotHoldStore.isSlotHeld("clinica-irma", "X", slot1000Start, slot1000End)).isTrue();

        // Advance clock by 301 seconds (> 300s TTL)
        slotHoldStore.advanceTimeSeconds(301L);

        assertThat(slotHoldStore.isSlotHeld("clinica-irma", "X", slot1000Start, slot1000End)).isFalse();
        assertThat(slotHoldStore.getActiveHoldByToken(hold.holdToken())).isEmpty();
    }

    @Test
    @DisplayName("[AHC-8 AC-4] Atomic PostgreSQL btree_gist exclusion violation aborts overlapping transaction with HTTP 409 Conflict")
    void shouldAbortOverlappingBookingTransactionWith409WhenDatabaseExclusionConstraintTriggers() {
        Instant slot1000Start = Instant.parse("2026-11-17T10:00:00Z");
        Instant slot1000End = Instant.parse("2026-11-17T10:30:00Z");

        SlotHoldResponse hold = schedulingService.acquireOptimisticSlotHold(
                new AcquireSlotHoldCommand("sede-norte", "X", slot1000Start, slot1000End, "turnstile-valid-token"),
                "203.0.113.70"
        );

        when(appointmentRepository.saveAndFlush(any(ScheduledAppointmentEntity.class)))
                .thenThrow(new DataIntegrityViolationException("ERROR: conflicting key value violates exclusion constraint \"no_scheduled_doctor_overlap\""));

        assertThatThrownBy(() -> schedulingService.confirmAppointment(
                new ConfirmAppointmentCommand(
                        "sede-norte",
                        "room-cardio-1",
                        "X",
                        "patient-102",
                        "Carlos Mendoza",
                        "+52 55 9988 7766",
                        hold.holdToken(),
                        slot1000Start,
                        slot1000End
                ),
                "Bearer valid-jwt-token"
        ))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException domainEx = (DomainException) ex;
                    assertThat(domainEx.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(domainEx.getTitle()).isEqualTo("Atomic Database Exclusion Conflict");
                });
    }
}
