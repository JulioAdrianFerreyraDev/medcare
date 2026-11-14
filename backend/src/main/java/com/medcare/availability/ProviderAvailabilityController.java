package com.medcare.availability;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * [AHC-6 & AHC-7] REST Controller for provider weekly shift templates, slot availability generation,
 * planned time-off blocks, emergency cancellations, receptionist alerts, and immutable audit logs.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Provider Availability & Exceptions")
public class ProviderAvailabilityController {

    private final ProviderAvailabilityService availabilityService;

    public ProviderAvailabilityController(ProviderAvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @PostMapping("/providers/{providerId}/shifts")
    @Operation(
            summary = "Configure recurring weekly shift windows and meal breaks for a provider at a branch",
            description = "[AHC-6 AC-1 & AC-2] Saves weekly shift windows per branch and rejects cross-branch schedule collisions on the same weekday with HTTP 422 Unprocessable Entity."
    )
    public ResponseEntity<List<ProviderAvailabilityService.ProviderShiftResponse>> configureWeeklyShifts(
            @PathVariable String providerId,
            @Valid @RequestBody ProviderAvailabilityService.ConfigureWeeklyShiftTemplateCommand command
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(availabilityService.configureWeeklyShifts(providerId, command));
    }

    @GetMapping("/providers/{providerId}/shifts")
    @Operation(summary = "List all configured weekly shift windows for a provider across branches")
    public ResponseEntity<List<ProviderAvailabilityService.ProviderShiftResponse>> listProviderShifts(
            @PathVariable String providerId
    ) {
        return ResponseEntity.ok(availabilityService.listProviderShifts(providerId));
    }

    @GetMapping("/providers/{providerId}/available-slots")
    @Operation(
            summary = "Generate real-time bookable slots for a provider on a specific date and branch",
            description = "[AHC-6 AC-1, AHC-7 AC-1, AHC-8 AC-1 & AC-3] Exposes bookable slots strictly within shift windows, blocking meal breaks, planned leave ranges, active 300s Redis holds, and confirmed appointments."
    )
    public ResponseEntity<List<ProviderAvailabilityService.BookableSlotResponse>> getAvailableSlots(
            @PathVariable String providerId,
            @RequestParam String branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer durationMinutes
    ) {
        return ResponseEntity.ok(availabilityService.getAvailableSlots(providerId, branchId, date, durationMinutes));
    }

    @PostMapping("/providers/{providerId}/time-off")
    @Operation(
            summary = "Register a planned leave block for future dates with zero booked appointments",
            description = "[AHC-7 AC-1 & AC-3] Immediately suppresses all public and internal slots in the date range and writes an immutable audit log entry."
    )
    public ResponseEntity<ProviderAvailabilityService.TimeOffBlockResponse> registerPlannedLeave(
            @PathVariable String providerId,
            @Valid @RequestBody ProviderAvailabilityService.PlannedLeaveCommand command
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(availabilityService.registerPlannedLeave(providerId, command));
    }

    @PostMapping("/providers/{providerId}/emergency-cancellations")
    @Operation(
            summary = "Trigger an urgent emergency cancellation block with a mandatory reason",
            description = "[AHC-7 AC-2 & AC-3] Transitions all affected SCHEDULED appointments to REQUIRES_RESCHEDULING, creates a HIGH-priority receptionist alert, notifies affected patients, and records an immutable audit log."
    )
    public ResponseEntity<ProviderAvailabilityService.TimeOffBlockResponse> triggerEmergencyCancellation(
            @PathVariable String providerId,
            @Valid @RequestBody ProviderAvailabilityService.EmergencyCancellationCommand command
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(availabilityService.triggerEmergencyCancellation(providerId, command));
    }

    @GetMapping("/providers/{providerId}/cancellation-audit-logs")
    @Operation(summary = "List immutable schedule block and cancellation audit log entries for a provider")
    public ResponseEntity<List<ProviderAvailabilityService.CancellationAuditLogResponse>> listCancellationAuditLogs(
            @PathVariable String providerId
    ) {
        return ResponseEntity.ok(availabilityService.listCancellationAuditLogs(providerId));
    }

    @GetMapping("/branches/{branchId}/receptionist-alerts")
    @Operation(summary = "List high-priority receptionist dashboard alerts for a branch")
    public ResponseEntity<List<ProviderAvailabilityService.ReceptionistAlertResponse>> listReceptionistAlerts(
            @PathVariable String branchId
    ) {
        return ResponseEntity.ok(availabilityService.listReceptionistAlertsForBranch(branchId));
    }
}
