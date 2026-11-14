package com.medcare.availability;

import com.medcare.scheduling.ScheduledAppointmentEntity;
import com.medcare.scheduling.ScheduledAppointmentRepository;
import com.medcare.scheduling.SlotHoldStorePort;
import com.medcare.shared.exception.DomainException;
import com.medcare.tenancy.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [AHC-6 & AHC-7] Domain service managing provider working shifts, cross-branch conflict detection,
 * real-time bookable slot generation, planned leave blocking, emergency cancellations, and immutable audit logs.
 */
@Service
public class ProviderAvailabilityService {

    private final ProviderShiftRepository shiftRepository;
    private final ProviderTimeOffBlockRepository timeOffBlockRepository;
    private final ScheduleCancellationAuditLogRepository auditLogRepository;
    private final ReceptionistDashboardAlertRepository receptionistAlertRepository;
    private final ScheduledAppointmentRepository scheduledAppointmentRepository;
    private final SlotHoldStorePort slotHoldStore;
    private final PatientReschedulingNotificationPort notificationPort;

    public ProviderAvailabilityService(
            ProviderShiftRepository shiftRepository,
            ProviderTimeOffBlockRepository timeOffBlockRepository,
            ScheduleCancellationAuditLogRepository auditLogRepository,
            ReceptionistDashboardAlertRepository receptionistAlertRepository,
            ScheduledAppointmentRepository scheduledAppointmentRepository,
            SlotHoldStorePort slotHoldStore,
            PatientReschedulingNotificationPort notificationPort
    ) {
        this.shiftRepository = shiftRepository;
        this.timeOffBlockRepository = timeOffBlockRepository;
        this.auditLogRepository = auditLogRepository;
        this.receptionistAlertRepository = receptionistAlertRepository;
        this.scheduledAppointmentRepository = scheduledAppointmentRepository;
        this.slotHoldStore = slotHoldStore;
        this.notificationPort = notificationPort;
    }

    public record ShiftWindowInput(
            @NotNull LocalTime startTime,
            @NotNull LocalTime endTime
    ) {
    }

    public record ConfigureWeeklyShiftTemplateCommand(
            @NotBlank String branchId,
            @NotBlank String branchName,
            @NotBlank String dayOfWeek,
            @Min(5) @Max(240) int slotIntervalMinutes,
            @NotEmpty List<@Valid ShiftWindowInput> windows
    ) {
    }

    public record ProviderShiftResponse(
            UUID id,
            String tenantSlug,
            String providerId,
            String branchId,
            String branchName,
            String dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            int slotIntervalMinutes
    ) {
    }

    public record BookableSlotResponse(
            String providerId,
            String branchId,
            LocalDate date,
            LocalTime localStartTime,
            LocalTime localEndTime,
            Instant startTimeUtc,
            Instant endTimeUtc,
            boolean available
    ) {
    }

    public record PlannedLeaveCommand(
            @NotBlank String branchId,
            @NotNull Instant startDateTime,
            @NotNull Instant endDateTime,
            @NotBlank String reason
    ) {
    }

    public record EmergencyCancellationCommand(
            @NotBlank String branchId,
            @NotNull Instant startDateTime,
            @NotNull Instant endDateTime,
            @NotBlank String reason
    ) {
    }

    public record TimeOffBlockResponse(
            UUID id,
            String tenantSlug,
            String providerId,
            String branchId,
            String blockType,
            Instant startDateTime,
            Instant endDateTime,
            String reason,
            int affectedAppointmentsCount,
            List<UUID> rescheduledAppointmentIds,
            UUID receptionistAlertId,
            UUID auditLogId
    ) {
    }

    public record CancellationAuditLogResponse(
            UUID id,
            String tenantSlug,
            String providerId,
            UUID blockId,
            String actionType,
            String reason,
            int affectedAppointmentsCount,
            Instant recordedAt
    ) {
    }

    public record ReceptionistAlertResponse(
            UUID id,
            String tenantSlug,
            String branchId,
            String providerId,
            String severity,
            String title,
            String message,
            int affectedAppointmentsCount,
            String status,
            Instant createdAt
    ) {
    }

    /**
     * [AHC-6 AC-1 & AC-2] Configures one or more working shift windows for a provider on a specific weekday and branch,
     * rejecting any cross-branch schedule collision with HTTP 422 Unprocessable Entity.
     */
    @Transactional
    public List<ProviderShiftResponse> configureWeeklyShifts(
            String providerId,
            ConfigureWeeklyShiftTemplateCommand command
    ) {
        String normalizedDay = normalizeDayOfWeek(command.dayOfWeek());
        int slotInterval = command.slotIntervalMinutes() > 0 ? command.slotIntervalMinutes() : 30;

        for (ShiftWindowInput window : command.windows()) {
            if (!window.endTime().isAfter(window.startTime())) {
                throw new DomainException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "Invalid Shift Window",
                        "Shift end time (" + window.endTime() + ") must be strictly after start time (" + window.startTime() + ")."
                );
            }
        }

        // Validate intra-request window overlaps
        List<ShiftWindowInput> sortedWindows = command.windows().stream()
                .sorted(Comparator.comparing(ShiftWindowInput::startTime))
                .toList();
        for (int i = 1; i < sortedWindows.size(); i++) {
            ShiftWindowInput prev = sortedWindows.get(i - 1);
            ShiftWindowInput curr = sortedWindows.get(i);
            if (curr.startTime().isBefore(prev.endTime())) {
                throw new DomainException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "Overlapping Shift Windows",
                        "Shift windows on the same day cannot overlap (" + prev.startTime() + "-" + prev.endTime()
                                + " overlaps with " + curr.startTime() + "-" + curr.endTime() + ")."
                );
            }
        }

        String tenantSlug = requireTenant();
        List<ProviderShiftEntity> existingDayShifts = shiftRepository.findAllByTenantSlugAndProviderIdAndDayOfWeek(
                tenantSlug,
                providerId,
                normalizedDay
        );

        // [AHC-6 AC-2] Check cross-branch shift collision
        for (ShiftWindowInput candidate : sortedWindows) {
            for (ProviderShiftEntity existing : existingDayShifts) {
                if (!existing.getBranchId().equalsIgnoreCase(command.branchId())) {
                    boolean overlaps = candidate.startTime().isBefore(existing.getEndTime())
                            && candidate.endTime().isAfter(existing.getStartTime());
                    if (overlaps) {
                        throw new DomainException(
                                HttpStatus.UNPROCESSABLE_ENTITY,
                                "Cross-Branch Shift Conflict",
                                String.format(
                                        "Cross-branch schedule conflict on %s: requested shift %s-%s at '%s' overlaps with existing shift %s-%s at '%s'.",
                                        normalizedDay,
                                        candidate.startTime(),
                                        candidate.endTime(),
                                        command.branchName(),
                                        existing.getStartTime(),
                                        existing.getEndTime(),
                                        existing.getBranchName()
                                )
                        );
                    }
                }
            }
        }

        // Replace existing shifts for this specific (provider, branch, dayOfWeek) template
        List<ProviderShiftEntity> sameBranchShifts = existingDayShifts.stream()
                .filter(s -> s.getBranchId().equalsIgnoreCase(command.branchId()))
                .toList();
        if (!sameBranchShifts.isEmpty()) {
            shiftRepository.deleteAll(sameBranchShifts);
        }

        List<ProviderShiftEntity> toSave = sortedWindows.stream()
                .map(window -> new ProviderShiftEntity(
                        UUID.randomUUID(),
                        tenantSlug,
                        providerId,
                        command.branchId(),
                        command.branchName(),
                        normalizedDay,
                        window.startTime(),
                        window.endTime(),
                        slotInterval
                ))
                .toList();

        return shiftRepository.saveAll(toSave).stream()
                .map(this::toShiftResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProviderShiftResponse> listProviderShifts(String providerId) {
        String tenantSlug = requireTenant();
        return shiftRepository.findAllByTenantSlugAndProviderId(tenantSlug, providerId)
                .stream()
                .sorted(Comparator.comparing(ProviderShiftEntity::getDayOfWeek)
                        .thenComparing(ProviderShiftEntity::getStartTime))
                .map(this::toShiftResponse)
                .toList();
    }

    /**
     * [AHC-6 AC-1, AHC-7 AC-1, AHC-8 AC-1 & AC-3] Generates bookable slots for a provider at a branch on a given date:
     * - Only generates intervals within configured shift windows (leaving meal breaks like 13:00-15:00 blocked).
     * - Suppresses any slot overlapping a planned leave or emergency block (AHC-7 AC-1 & AC-2).
     * - Hides any slot currently held in Redis (AHC-8 AC-1) until the 300s TTL expires (AHC-8 AC-3).
     * - Excludes any slot overlapping a SCHEDULED appointment in PostgreSQL.
     */
    @Transactional(readOnly = true)
    public List<BookableSlotResponse> getAvailableSlots(
            String providerId,
            String branchId,
            LocalDate date,
            Integer requestedDurationMinutes
    ) {
        String tenantSlug = requireTenant();
        String dayOfWeek = date.getDayOfWeek().name();

        List<ProviderShiftEntity> shifts = shiftRepository.findAllByTenantSlugAndProviderIdAndBranchIdAndDayOfWeek(
                tenantSlug,
                providerId,
                branchId,
                dayOfWeek
        ).stream()
                .sorted(Comparator.comparing(ProviderShiftEntity::getStartTime))
                .toList();

        if (shifts.isEmpty()) {
            return List.of();
        }

        Instant dayStartUtc = date.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant dayEndUtc = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<ProviderTimeOffBlockEntity> activeBlocks = timeOffBlockRepository.findOverlappingBlocks(
                tenantSlug,
                providerId,
                dayStartUtc,
                dayEndUtc
        );

        List<ScheduledAppointmentEntity> bookedAppointments = scheduledAppointmentRepository.findOverlappingScheduledByDoctor(
                tenantSlug,
                providerId,
                dayStartUtc,
                dayEndUtc
        );

        List<BookableSlotResponse> availableSlots = new ArrayList<>();

        for (ProviderShiftEntity shift : shifts) {
            int intervalMinutes = (requestedDurationMinutes != null && requestedDurationMinutes > 0)
                    ? requestedDurationMinutes
                    : shift.getSlotIntervalMinutes();

            LocalTime cursor = shift.getStartTime();
            while (!cursor.plusMinutes(intervalMinutes).isAfter(shift.getEndTime())) {
                LocalTime slotEnd = cursor.plusMinutes(intervalMinutes);
                Instant slotStartUtc = date.atTime(cursor).toInstant(ZoneOffset.UTC);
                Instant slotEndUtc = date.atTime(slotEnd).toInstant(ZoneOffset.UTC);

                boolean blockedByTimeOff = activeBlocks.stream()
                        .anyMatch(block -> slotStartUtc.isBefore(block.getEndDateTime())
                                && slotEndUtc.isAfter(block.getStartDateTime()));

                boolean blockedByConfirmedAppointment = bookedAppointments.stream()
                        .anyMatch(appt -> slotStartUtc.isBefore(appt.getEndTime())
                                && slotEndUtc.isAfter(appt.getStartTime()));

                boolean blockedByOptimisticHold = slotHoldStore.isSlotHeld(
                        tenantSlug,
                        providerId,
                        slotStartUtc,
                        slotEndUtc
                );

                if (!blockedByTimeOff && !blockedByConfirmedAppointment && !blockedByOptimisticHold) {
                    availableSlots.add(new BookableSlotResponse(
                            providerId,
                            branchId,
                            date,
                            cursor,
                            slotEnd,
                            slotStartUtc,
                            slotEndUtc,
                            true
                    ));
                }

                cursor = slotEnd;
            }
        }

        return availableSlots;
    }

    /**
     * [AHC-7 AC-1 & AC-3] Registers a planned leave date-range block when zero appointments are booked,
     * immediately suppressing all public/internal slots in that range and recording an immutable audit entry.
     */
    @Transactional
    public TimeOffBlockResponse registerPlannedLeave(String providerId, PlannedLeaveCommand command) {
        validateTimeOffRangeAndReason(command.startDateTime(), command.endDateTime(), command.reason());
        String tenantSlug = requireTenant();

        List<ScheduledAppointmentEntity> overlappingAppointments =
                scheduledAppointmentRepository.findOverlappingScheduledByDoctor(
                        tenantSlug,
                        providerId,
                        command.startDateTime(),
                        command.endDateTime()
                );

        if (!overlappingAppointments.isEmpty()) {
            throw new DomainException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Planned Leave Has Active Appointments",
                    "Cannot register standard planned leave while " + overlappingAppointments.size()
                            + " scheduled appointments exist in the range. Trigger an emergency cancellation or reschedule first."
            );
        }

        ProviderTimeOffBlockEntity savedBlock = timeOffBlockRepository.save(new ProviderTimeOffBlockEntity(
                UUID.randomUUID(),
                tenantSlug,
                providerId,
                command.branchId(),
                "PLANNED_LEAVE",
                command.startDateTime(),
                command.endDateTime(),
                command.reason().trim(),
                0
        ));

        ScheduleCancellationAuditLogEntity auditLog = auditLogRepository.save(new ScheduleCancellationAuditLogEntity(
                UUID.randomUUID(),
                tenantSlug,
                providerId,
                savedBlock.getId(),
                "PLANNED_LEAVE",
                command.reason().trim(),
                0,
                Instant.now()
        ));

        return new TimeOffBlockResponse(
                savedBlock.getId(),
                tenantSlug,
                providerId,
                command.branchId(),
                savedBlock.getBlockType(),
                savedBlock.getStartDateTime(),
                savedBlock.getEndDateTime(),
                savedBlock.getReason(),
                0,
                List.of(),
                null,
                auditLog.getId()
        );
    }

    /**
     * [AHC-7 AC-2 & AC-3] Executes an emergency cancellation block with a mandatory reason:
     * - Transitions all overlapping SCHEDULED appointments to REQUIRES_RESCHEDULING.
     * - Creates a HIGH-priority alert on the branch receptionist dashboard.
     * - Dispatches automated notifications to all affected patients.
     * - Records an immutable audit trail entry.
     */
    @Transactional
    public TimeOffBlockResponse triggerEmergencyCancellation(
            String providerId,
            EmergencyCancellationCommand command
    ) {
        validateTimeOffRangeAndReason(command.startDateTime(), command.endDateTime(), command.reason());
        String tenantSlug = requireTenant();
        String trimmedReason = command.reason().trim();

        List<ScheduledAppointmentEntity> affectedAppointments =
                scheduledAppointmentRepository.findOverlappingScheduledByDoctor(
                        tenantSlug,
                        providerId,
                        command.startDateTime(),
                        command.endDateTime()
                );

        List<UUID> affectedIds = new ArrayList<>();
        for (ScheduledAppointmentEntity appointment : affectedAppointments) {
            appointment.setStatus("REQUIRES_RESCHEDULING");
            affectedIds.add(appointment.getId());
            notificationPort.notifyPatientOfEmergencyCancellation(
                    tenantSlug,
                    appointment.getId(),
                    appointment.getPatientName(),
                    appointment.getPatientContact(),
                    providerId,
                    trimmedReason
            );
        }
        if (!affectedAppointments.isEmpty()) {
            scheduledAppointmentRepository.saveAll(affectedAppointments);
        }

        ProviderTimeOffBlockEntity savedBlock = timeOffBlockRepository.save(new ProviderTimeOffBlockEntity(
                UUID.randomUUID(),
                tenantSlug,
                providerId,
                command.branchId(),
                "EMERGENCY_CANCELLATION",
                command.startDateTime(),
                command.endDateTime(),
                trimmedReason,
                affectedAppointments.size()
        ));

        ReceptionistDashboardAlertEntity alert = receptionistAlertRepository.save(new ReceptionistDashboardAlertEntity(
                UUID.randomUUID(),
                tenantSlug,
                command.branchId(),
                providerId,
                "HIGH",
                "Emergency Provider Cancellation — Rescheduling Required",
                String.format(
                        "Provider '%s' triggered an emergency cancellation (%s). %d scheduled appointment(s) transitioned to REQUIRES_RESCHEDULING.",
                        providerId,
                        trimmedReason,
                        affectedAppointments.size()
                ),
                affectedAppointments.size(),
                "OPEN"
        ));

        ScheduleCancellationAuditLogEntity auditLog = auditLogRepository.save(new ScheduleCancellationAuditLogEntity(
                UUID.randomUUID(),
                tenantSlug,
                providerId,
                savedBlock.getId(),
                "EMERGENCY_CANCELLATION",
                trimmedReason,
                affectedAppointments.size(),
                Instant.now()
        ));

        return new TimeOffBlockResponse(
                savedBlock.getId(),
                tenantSlug,
                providerId,
                command.branchId(),
                savedBlock.getBlockType(),
                savedBlock.getStartDateTime(),
                savedBlock.getEndDateTime(),
                savedBlock.getReason(),
                affectedAppointments.size(),
                affectedIds,
                alert.getId(),
                auditLog.getId()
        );
    }

    @Transactional(readOnly = true)
    public List<CancellationAuditLogResponse> listCancellationAuditLogs(String providerId) {
        String tenantSlug = requireTenant();
        return auditLogRepository.findAllByTenantSlugAndProviderIdOrderByRecordedAtDesc(tenantSlug, providerId)
                .stream()
                .map(entry -> new CancellationAuditLogResponse(
                        entry.getId(),
                        entry.getTenantSlug(),
                        entry.getProviderId(),
                        entry.getBlockId(),
                        entry.getActionType(),
                        entry.getReason(),
                        entry.getAffectedAppointmentsCount(),
                        entry.getRecordedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReceptionistAlertResponse> listReceptionistAlertsForBranch(String branchId) {
        String tenantSlug = requireTenant();
        return receptionistAlertRepository.findAllByTenantSlugAndBranchIdOrderByCreatedAtDesc(tenantSlug, branchId)
                .stream()
                .map(alert -> new ReceptionistAlertResponse(
                        alert.getId(),
                        alert.getTenantSlug(),
                        alert.getBranchId(),
                        alert.getProviderId(),
                        alert.getSeverity(),
                        alert.getTitle(),
                        alert.getMessage(),
                        alert.getAffectedAppointmentsCount(),
                        alert.getStatus(),
                        alert.getCreatedAt()
                ))
                .toList();
    }

    private void validateTimeOffRangeAndReason(Instant startDateTime, Instant endDateTime, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new DomainException(
                    HttpStatus.BAD_REQUEST,
                    "Cancellation Reason Required",
                    "A non-empty reason is mandatory when recording a schedule block or emergency cancellation."
            );
        }
        if (startDateTime == null || endDateTime == null || !endDateTime.isAfter(startDateTime)) {
            throw new DomainException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid Date Range",
                    "Block end date/time must be strictly after start date/time."
            );
        }
    }

    private String normalizeDayOfWeek(String dayOfWeek) {
        try {
            return DayOfWeek.valueOf(dayOfWeek.trim().toUpperCase()).name();
        } catch (Exception ex) {
            throw new DomainException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid Day of Week",
                    "Day of week '" + dayOfWeek + "' is not a valid ISO day of week (MONDAY..SUNDAY)."
            );
        }
    }

    private ProviderShiftResponse toShiftResponse(ProviderShiftEntity entity) {
        return new ProviderShiftResponse(
                entity.getId(),
                entity.getTenantSlug(),
                entity.getProviderId(),
                entity.getBranchId(),
                entity.getBranchName(),
                entity.getDayOfWeek(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getSlotIntervalMinutes()
        );
    }

    private String requireTenant() {
        return TenantContext.getCurrentTenantOrDefault("default-tenant");
    }
}
