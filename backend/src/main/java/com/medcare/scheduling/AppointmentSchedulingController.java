package com.medcare.scheduling;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * [AHC-8, AHC-45, AHC-46, AHC-47] REST Controller for optimistic slot holds, appointment confirmation,
 * multi-doctor calendar matrix, patient check-in & clinic flow state transitions, and public booking with OTP.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Scheduling & Clinic Flow")
public class AppointmentSchedulingController {

    private final OptimisticSchedulingService schedulingService;

    public AppointmentSchedulingController(OptimisticSchedulingService schedulingService) {
        this.schedulingService = schedulingService;
    }

    // =========================================================================
    // Core Booking & Holds (AHC-8)
    // =========================================================================

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

    // =========================================================================
    // Multi-Doctor Calendar Matrix & Rescheduling (AHC-45)
    // =========================================================================

    @GetMapping("/branches/{branchId}/calendar")
    @Operation(
            summary = "Query multi-doctor daily calendar matrix for a branch",
            description = "[AHC-45 AC-3.1.1 & AC-3.1.2] Retrieves all appointments for the specified branch and date, organized as parallel provider columns with optional specialty and doctor filters."
    )
    public ResponseEntity<OptimisticSchedulingService.BranchDailyCalendarResponse> getBranchCalendar(
            @PathVariable String branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String specialty,
            @RequestParam(required = false) String doctorId
    ) {
        return ResponseEntity.ok(schedulingService.getBranchCalendar(branchId, date, specialty, doctorId));
    }

    @PutMapping("/appointments/{id}/reschedule")
    @Operation(
            summary = "Reschedule an existing appointment to a new time window or room/doctor",
            description = "[AHC-45 AC-3.1.3] Validates slot availability against provider and room schedules, ensuring no double-booking collision (excluding the appointment's own id), and updates the appointment atomically."
    )
    public ResponseEntity<OptimisticSchedulingService.ScheduledAppointmentResponse> rescheduleAppointment(
            @PathVariable UUID id,
            @Valid @RequestBody OptimisticSchedulingService.RescheduleAppointmentCommand command
    ) {
        return ResponseEntity.ok(schedulingService.rescheduleAppointment(id, command));
    }

    // =========================================================================
    // Clinic Flow Tracking & Status State Machine (AHC-46)
    // =========================================================================

    @PatchMapping("/appointments/{id}/status")
    @Operation(
            summary = "Transition appointment status in the clinic flow state machine",
            description = "[AHC-46 AC-46.1, AC-46.2, AC-46.3] Enforces strict state transitions (SCHEDULED -> WAITING_IN_LOBBY -> IN_CONSULTATION -> COMPLETED, or CANCELLED) and records timestamps."
    )
    public ResponseEntity<OptimisticSchedulingService.ScheduledAppointmentResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody OptimisticSchedulingService.UpdateAppointmentStatusCommand command
    ) {
        return ResponseEntity.ok(schedulingService.transitionAppointmentStatus(id, command.status()));
    }

    @PostMapping("/appointments/{id}/check-in")
    @Operation(
            summary = "Check in patient upon lobby arrival",
            description = "[AHC-46 AC-46.1] Transitions status from SCHEDULED to WAITING_IN_LOBBY and records checkedInAt timestamp."
    )
    public ResponseEntity<OptimisticSchedulingService.ScheduledAppointmentResponse> checkIn(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(schedulingService.checkInPatient(id));
    }

    @PostMapping("/appointments/{id}/start-consultation")
    @Operation(
            summary = "Start doctor consultation with waiting patient",
            description = "[AHC-46 AC-46.2] Transitions status from WAITING_IN_LOBBY to IN_CONSULTATION and records consultationStartedAt timestamp."
    )
    public ResponseEntity<OptimisticSchedulingService.ScheduledAppointmentResponse> startConsultation(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(schedulingService.startConsultation(id));
    }

    @PostMapping("/appointments/{id}/complete")
    @Operation(
            summary = "Complete doctor consultation and checkout patient",
            description = "[AHC-46 AC-46.3] Transitions status from IN_CONSULTATION to COMPLETED and records consultationCompletedAt timestamp."
    )
    public ResponseEntity<OptimisticSchedulingService.ScheduledAppointmentResponse> completeConsultation(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(schedulingService.completeConsultation(id));
    }

    // =========================================================================
    // Public Booking Portal & OTP Login (AHC-47)
    // =========================================================================

    @GetMapping("/public/slots")
    @Operation(
            summary = "Discover real-time bookable slots publicly across doctors in a branch",
            description = "[AHC-47 AC-47.1] Returns public slots for a given branch and date, crossing working shifts, time-off, active holds, and booked appointments."
    )
    public ResponseEntity<List<OptimisticSchedulingService.PublicBookableSlotResponse>> discoverPublicSlots(
            @RequestParam String branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String doctorId,
            @RequestParam(required = false) String specialty,
            @RequestParam(required = false) Integer durationMinutes
    ) {
        return ResponseEntity.ok(schedulingService.discoverPublicSlots(branchId, date, doctorId, specialty, durationMinutes));
    }

    @PostMapping("/public/auth/otp/send")
    @Operation(
            summary = "Dispatch simulated 6-digit OTP code to patient contact",
            description = "[AHC-47 AC-47.2] Dispatches a 6-digit OTP code valid for 300 seconds to the provided phone number or email."
    )
    public ResponseEntity<OptimisticSchedulingService.SendOtpResponse> sendOtp(
            @Valid @RequestBody OptimisticSchedulingService.SendOtpCommand command
    ) {
        return ResponseEntity.ok(schedulingService.sendOtp(command));
    }

    @PostMapping("/public/auth/otp/verify")
    @Operation(
            summary = "Verify OTP code and receive authenticated patient JWT",
            description = "[AHC-47 AC-47.3] Validates the 6-digit OTP code and returns a Bearer JWT token for booking confirmation."
    )
    public ResponseEntity<OptimisticSchedulingService.VerifyOtpResponse> verifyOtp(
            @Valid @RequestBody OptimisticSchedulingService.VerifyOtpCommand command
    ) {
        return ResponseEntity.ok(schedulingService.verifyOtp(command));
    }

    @PostMapping("/public/bookings/confirm")
    @Operation(
            summary = "Confirm self-service public booking with optimistic hold and patient token",
            description = "[AHC-47 AC-47.3] Validates Bearer JWT and matching holdToken, persists the appointment as SCHEDULED, and releases the Redis lock."
    )
    public ResponseEntity<OptimisticSchedulingService.ScheduledAppointmentResponse> confirmPublicBooking(
            @Valid @RequestBody OptimisticSchedulingService.PublicBookingConfirmationCommand command,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(schedulingService.confirmPublicBooking(command, authorizationHeader));
    }
}
