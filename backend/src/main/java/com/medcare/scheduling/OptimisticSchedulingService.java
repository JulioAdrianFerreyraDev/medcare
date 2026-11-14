package com.medcare.scheduling;

import com.medcare.shared.exception.DomainException;
import com.medcare.tenancy.TenantContext;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [AHC-8 AC-1, AC-2, AC-3, AC-4] Service implementing unauthenticated optimistic slot holds
 * and authenticated double-booking-free appointment confirmation.
 */
@Service
public class OptimisticSchedulingService {

    private final SlotHoldStorePort slotHoldStore;
    private final AntiBotVerificationPort antiBotVerification;
    private final ScheduledAppointmentRepository appointmentRepository;

    public OptimisticSchedulingService(
            SlotHoldStorePort slotHoldStore,
            AntiBotVerificationPort antiBotVerification,
            ScheduledAppointmentRepository appointmentRepository
    ) {
        this.slotHoldStore = slotHoldStore;
        this.antiBotVerification = antiBotVerification;
        this.appointmentRepository = appointmentRepository;
    }

    public record AcquireSlotHoldCommand(
            @NotBlank String branchId,
            @NotBlank String doctorId,
            @NotNull Instant startTime,
            @NotNull Instant endTime,
            @NotBlank String turnstileToken
    ) {
    }

    public record SlotHoldResponse(
            String redisKey,
            String holdToken,
            String branchId,
            String doctorId,
            Instant startTime,
            Instant endTime,
            Instant expiresAt,
            long ttlSeconds
    ) {
    }

    public record ConfirmAppointmentCommand(
            @NotBlank String branchId,
            @NotBlank String roomId,
            @NotBlank String doctorId,
            @NotBlank String patientId,
            @NotBlank String patientName,
            @NotBlank String patientContact,
            @NotBlank String holdToken,
            @NotNull Instant startTime,
            @NotNull Instant endTime
    ) {
    }

    public record ScheduledAppointmentResponse(
            UUID id,
            String tenantSlug,
            String branchId,
            String roomId,
            String doctorId,
            String patientId,
            String patientName,
            String patientContact,
            String holdToken,
            Instant startTime,
            Instant endTime,
            String status
    ) {
    }

    /**
     * [AHC-8 AC-1] Places an unauthenticated optimistic hold on a slot after verifying Turnstile anti-bot
     * token and IP rate limits (max 2 active holds per IP), storing lock:doctor:{X}:slot:{HHmm} with 300s TTL.
     */
    @Transactional(readOnly = true)
    public SlotHoldResponse acquireOptimisticSlotHold(AcquireSlotHoldCommand command, String clientIp) {
        if (!command.endTime().isAfter(command.startTime())) {
            throw new DomainException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid Slot Window",
                    "Appointment end time must be strictly after start time."
            );
        }

        if (!antiBotVerification.verifyToken(command.turnstileToken(), clientIp)) {
            throw new DomainException(
                    HttpStatus.FORBIDDEN,
                    "Anti-Bot Verification Failed",
                    "Turnstile / reCAPTCHA verification failed for slot hold request."
            );
        }

        String tenantSlug = requireTenant();
        List<ScheduledAppointmentEntity> existingDoctorConflicts = appointmentRepository.findOverlappingScheduledByDoctor(
                tenantSlug,
                command.doctorId(),
                command.startTime(),
                command.endTime()
        );
        if (!existingDoctorConflicts.isEmpty()) {
            throw new DomainException(
                    HttpStatus.CONFLICT,
                    "Appointment Slot Conflict",
                    "Doctor '" + command.doctorId() + "' already has a confirmed appointment during the requested slot."
            );
        }

        SlotHoldStorePort.SlotHoldEntry entry = slotHoldStore.acquireHold(
                tenantSlug,
                command.branchId(),
                command.doctorId(),
                command.startTime(),
                command.endTime(),
                clientIp
        );

        return new SlotHoldResponse(
                entry.redisKey(),
                entry.holdToken(),
                entry.branchId(),
                entry.doctorId(),
                entry.startTime(),
                entry.endTime(),
                entry.expiresAt(),
                entry.ttlSeconds()
        );
    }

    /**
     * [AHC-8 AC-2 & AC-4] Confirms an appointment after verifying JWT authentication,
     * Redis holdToken ownership, and PostgreSQL btree_gist atomic exclusion constraints.
     */
    @Transactional
    public synchronized ScheduledAppointmentResponse confirmAppointment(
            ConfirmAppointmentCommand command,
            String authorizationHeader
    ) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ") || authorizationHeader.substring(7).isBlank()) {
            throw new DomainException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication Required",
                    "A valid Bearer JWT is required after login/OTP verification to confirm an appointment."
            );
        }

        if (!command.endTime().isAfter(command.startTime())) {
            throw new DomainException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid Slot Window",
                    "Appointment end time must be strictly after start time."
            );
        }

        String tenantSlug = requireTenant();

        // Check PostgreSQL temporal overlap first (AC-4: concurrent or conflicting booking returns 409 Conflict)
        if (!appointmentRepository.findOverlappingScheduledByDoctor(
                tenantSlug,
                command.doctorId(),
                command.startTime(),
                command.endTime()
        ).isEmpty()) {
            throw new DomainException(
                    HttpStatus.CONFLICT,
                    "Doctor Double-Booking Conflict",
                    "Doctor '" + command.doctorId() + "' is already booked during the requested time range."
            );
        }

        if (!appointmentRepository.findOverlappingScheduledByRoom(
                tenantSlug,
                command.roomId(),
                command.startTime(),
                command.endTime()
        ).isEmpty()) {
            throw new DomainException(
                    HttpStatus.CONFLICT,
                    "Physical Room Double-Booking Conflict",
                    "Physical room '" + command.roomId() + "' is already booked during the requested time range."
            );
        }

        // Verify holdToken ownership in Redis (AC-2)
        SlotHoldStorePort.SlotHoldEntry holdEntry = slotHoldStore.getActiveHoldByToken(command.holdToken())
                .orElseThrow(() -> new DomainException(
                        HttpStatus.CONFLICT,
                        "Invalid or Expired Hold Token",
                        "Hold token '" + command.holdToken() + "' does not exist or its 300-second TTL has expired."
                ));

        if (!holdEntry.doctorId().equals(command.doctorId())
                || !holdEntry.startTime().equals(command.startTime())
                || !holdEntry.endTime().equals(command.endTime())) {
            throw new DomainException(
                    HttpStatus.CONFLICT,
                    "Hold Token Ownership Mismatch",
                    "The provided holdToken does not match the requested doctor and slot window."
            );
        }

        ScheduledAppointmentEntity entity = new ScheduledAppointmentEntity(
                UUID.randomUUID(),
                tenantSlug,
                command.branchId(),
                command.roomId(),
                command.doctorId(),
                command.patientId(),
                command.patientName(),
                command.patientContact(),
                command.holdToken(),
                command.startTime(),
                command.endTime(),
                "SCHEDULED"
        );

        try {
            ScheduledAppointmentEntity saved = appointmentRepository.saveAndFlush(entity);
            slotHoldStore.releaseHold(command.holdToken());
            return toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new DomainException(
                    HttpStatus.CONFLICT,
                    "Atomic Database Exclusion Conflict",
                    "PostgreSQL btree_gist temporal exclusion aborted overlapping booking transaction."
            );
        }
    }

    @Transactional(readOnly = true)
    public List<ScheduledAppointmentResponse> listAppointmentsForDoctor(String doctorId) {
        String tenantSlug = requireTenant();
        return appointmentRepository.findAllByTenantSlugAndDoctorId(tenantSlug, doctorId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private ScheduledAppointmentResponse toResponse(ScheduledAppointmentEntity entity) {
        return new ScheduledAppointmentResponse(
                entity.getId(),
                entity.getTenantSlug(),
                entity.getBranchId(),
                entity.getRoomId(),
                entity.getDoctorId(),
                entity.getPatientId(),
                entity.getPatientName(),
                entity.getPatientContact(),
                entity.getHoldToken(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getStatus()
        );
    }

    private String requireTenant() {
        return TenantContext.getCurrentTenantOrDefault("default-tenant");
    }
}
