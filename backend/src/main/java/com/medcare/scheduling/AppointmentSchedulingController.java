package com.medcare.scheduling;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * [AHC-8] REST Controller for optimistic unauthenticated slot holds and authenticated appointment confirmation.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Optimistic Slot Hold & Scheduling")
public class AppointmentSchedulingController {

    private final OptimisticSchedulingService schedulingService;

    public AppointmentSchedulingController(OptimisticSchedulingService schedulingService) {
        this.schedulingService = schedulingService;
    }

    @PostMapping("/slots/hold")
    @Operation(
            summary = "Place an unauthenticated 300-second optimistic hold on a slot",
            description = "[AHC-8 AC-1 & AC-3] Verifies Turnstile anti-bot token and IP rate limits (max 2 active holds per IP), creates Redis lock `lock:doctor:{id}:slot:{HHmm}` mapped to `holdToken` with 300s TTL, and hides the slot from other users."
    )
    public ResponseEntity<OptimisticSchedulingService.SlotHoldResponse> acquireSlotHold(
            @Valid @RequestBody OptimisticSchedulingService.AcquireSlotHoldCommand command,
            @RequestHeader(value = "X-Forwarded-For", required = false) String forwardedFor,
            HttpServletRequest request
    ) {
        String clientIp = (forwardedFor != null && !forwardedFor.isBlank())
                ? forwardedFor.split(",")[0].trim()
                : request.getRemoteAddr();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(schedulingService.acquireOptimisticSlotHold(command, clientIp));
    }

    @PostMapping("/appointments")
    @Operation(
            summary = "Confirm an appointment using a valid Bearer JWT and matching holdToken",
            description = "[AHC-8 AC-2 & AC-4] Verifies Redis lock ownership, persists the appointment in PostgreSQL as SCHEDULED with btree_gist temporal exclusion, deletes the Redis lock, and returns 201 Created."
    )
    public ResponseEntity<OptimisticSchedulingService.ScheduledAppointmentResponse> confirmAppointment(
            @Valid @RequestBody OptimisticSchedulingService.ConfirmAppointmentCommand command,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(schedulingService.confirmAppointment(command, authorizationHeader));
    }

    @GetMapping("/providers/{doctorId}/scheduled-appointments")
    @Operation(summary = "List confirmed and rescheduling-pending appointments for a doctor")
    public ResponseEntity<List<OptimisticSchedulingService.ScheduledAppointmentResponse>> listDoctorAppointments(
            @PathVariable String doctorId
    ) {
        return ResponseEntity.ok(schedulingService.listAppointmentsForDoctor(doctorId));
    }
}
