package com.medcare.scheduling;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medcare.availability.ProviderShiftEntity;
import com.medcare.availability.ProviderShiftRepository;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * [AHC-45, AHC-46, AHC-47] Spring Boot MockMvc Integration Tests for:
 * - Multi-Doctor Calendar Matrix & Rescheduling (AHC-45)
 * - Patient Check-In & Clinic Flow State Transitions (AHC-46)
 * - Patient Self-Service Discovery, OTP Authentication & Booking Confirmation (AHC-47)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AppointmentClinicFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisSlotHoldStoreAdapter slotHoldStore;

    @Autowired
    private ScheduledAppointmentRepository appointmentRepository;

    @Autowired
    private ProviderShiftRepository shiftRepository;

    @BeforeEach
    void setUp() {
        slotHoldStore.resetAll();
        appointmentRepository.deleteAll();
        shiftRepository.deleteAll();

        // Setup Tuesday shift for Dr. Alejandro Vega at sede-norte
        shiftRepository.save(new ProviderShiftEntity(
                UUID.randomUUID(),
                "clinica-irma",
                "dr-alejandro-vega",
                "sede-norte",
                "Sede Norte",
                "TUESDAY",
                LocalTime.of(9, 0),
                LocalTime.of(13, 0),
                30
        ));
    }

    @AfterEach
    void tearDown() {
        slotHoldStore.resetAll();
        appointmentRepository.deleteAll();
        shiftRepository.deleteAll();
    }

    // =========================================================================
    // [AHC-45] Multi-Doctor Calendar Matrix & Rescheduling Tests
    // =========================================================================

    @Test
    @DisplayName("[AHC-45 AC-3.1.1 & AC-3.1.2] Query branch daily calendar matrix returns doctor columns and appointments")
    void shouldReturnBranchCalendarMatrixWithDoctorColumns() throws Exception {
        UUID apptId = UUID.randomUUID();
        appointmentRepository.save(new ScheduledAppointmentEntity(
                apptId,
                "clinica-irma",
                "sede-norte",
                "room-cardio-1",
                "dr-alejandro-vega",
                "pat-001",
                "Esteban Quito",
                "+52 55 1234 5678",
                "Consulta Cardiología",
                "Cardiology",
                "hold-test-1",
                Instant.parse("2026-11-17T09:00:00Z"),
                Instant.parse("2026-11-17T09:30:00Z"),
                "SCHEDULED",
                null,
                null,
                null
        ));

        // 2026-11-17 is a TUESDAY
        mockMvc.perform(get("/api/v1/branches/sede-norte/calendar")
                        .header("Host", "clinica-irma.medika.com")
                        .param("date", "2026-11-17"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.branchId", is("sede-norte")))
                .andExpect(jsonPath("$.date", is("2026-11-17")))
                .andExpect(jsonPath("$.totalAppointments", is(1)))
                .andExpect(jsonPath("$.doctors", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.doctors[0].doctorId", is("dr-alejandro-vega")))
                .andExpect(jsonPath("$.appointments[0].id", is(apptId.toString())))
                .andExpect(jsonPath("$.appointments[0].specialty", is("Cardiology")));
    }

    @Test
    @DisplayName("[AHC-45 AC-3.1.3] Drag-and-drop reschedule updates appointment slot times and returns 409 Conflict upon collision")
    void shouldRescheduleAppointmentAndDetectCollision() throws Exception {
        UUID apptId1 = UUID.randomUUID();
        appointmentRepository.save(new ScheduledAppointmentEntity(
                apptId1,
                "clinica-irma",
                "sede-norte",
                "room-cardio-1",
                "dr-alejandro-vega",
                "pat-001",
                "Esteban Quito",
                "+52 55 1234 5678",
                "Consulta Cardiología",
                "Cardiology",
                "hold-test-1",
                Instant.parse("2026-11-17T09:00:00Z"),
                Instant.parse("2026-11-17T09:30:00Z"),
                "SCHEDULED",
                null,
                null,
                null
        ));

        UUID apptId2 = UUID.randomUUID();
        appointmentRepository.save(new ScheduledAppointmentEntity(
                apptId2,
                "clinica-irma",
                "sede-norte",
                "room-cardio-1",
                "dr-alejandro-vega",
                "pat-002",
                "Ana Lucía",
                "+52 55 9876 5432",
                "Consulta Cardiología",
                "Cardiology",
                "hold-test-2",
                Instant.parse("2026-11-17T11:00:00Z"),
                Instant.parse("2026-11-17T11:30:00Z"),
                "SCHEDULED",
                null,
                null,
                null
        ));

        // 1. Reschedule appt1 to 10:00 AM (free slot) -> 200 OK
        String validReschedulePayload = """
                {
                  "newStartTime": "2026-11-17T10:00:00Z",
                  "newEndTime": "2026-11-17T10:30:00Z"
                }
                """;

        mockMvc.perform(put("/api/v1/appointments/" + apptId1 + "/reschedule")
                        .header("Host", "clinica-irma.medika.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validReschedulePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(apptId1.toString())))
                .andExpect(jsonPath("$.startTime", is("2026-11-17T10:00:00Z")))
                .andExpect(jsonPath("$.endTime", is("2026-11-17T10:30:00Z")));

        // 2. Reschedule appt1 to 11:00 AM (already booked by appt2) -> 409 Conflict
        String conflictingReschedulePayload = """
                {
                  "newStartTime": "2026-11-17T11:00:00Z",
                  "newEndTime": "2026-11-17T11:30:00Z"
                }
                """;

        mockMvc.perform(put("/api/v1/appointments/" + apptId1 + "/reschedule")
                        .header("Host", "clinica-irma.medika.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(conflictingReschedulePayload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title", is("Doctor Double-Booking Conflict")));
    }

    // =========================================================================
    // [AHC-46] Patient Check-In & Clinic Flow Tracking Tests
    // =========================================================================

    @Test
    @DisplayName("[AHC-46 AC-46.1, AC-46.2, AC-46.3] Full patient flow: SCHEDULED -> WAITING_IN_LOBBY -> IN_CONSULTATION -> COMPLETED")
    void shouldExecuteCompleteClinicFlowLifecycle() throws Exception {
        UUID apptId = UUID.randomUUID();
        appointmentRepository.save(new ScheduledAppointmentEntity(
                apptId,
                "clinica-irma",
                "sede-norte",
                "room-cardio-1",
                "dr-alejandro-vega",
                "pat-001",
                "Esteban Quito",
                "+52 55 1234 5678",
                "Consulta Cardiología",
                "Cardiology",
                "hold-test-flow",
                Instant.parse("2026-11-17T09:00:00Z"),
                Instant.parse("2026-11-17T09:30:00Z"),
                "SCHEDULED",
                null,
                null,
                null
        ));

        // 1. Reception Check-In (AC-46.1)
        mockMvc.perform(post("/api/v1/appointments/" + apptId + "/check-in")
                        .header("Host", "clinica-irma.medika.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("WAITING_IN_LOBBY")))
                .andExpect(jsonPath("$.checkedInAt", notNullValue()));

        // 2. Doctor Calls Patient / Start Consultation (AC-46.2)
        mockMvc.perform(post("/api/v1/appointments/" + apptId + "/start-consultation")
                        .header("Host", "clinica-irma.medika.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_CONSULTATION")))
                .andExpect(jsonPath("$.consultationStartedAt", notNullValue()));

        // 3. Doctor Completes Consultation (AC-46.3)
        mockMvc.perform(post("/api/v1/appointments/" + apptId + "/complete")
                        .header("Host", "clinica-irma.medika.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.consultationCompletedAt", notNullValue()));

        // 4. Illegal transition after completion returns 400 Bad Request
        mockMvc.perform(patch("/api/v1/appointments/" + apptId + "/status")
                        .header("Host", "clinica-irma.medika.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"WAITING_IN_LOBBY\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Invalid Status Transition")));
    }

    // =========================================================================
    // [AHC-47] Public Booking Discovery & OTP Login Tests
    // =========================================================================

    @Test
    @DisplayName("[AHC-47 AC-47.1, AC-47.2, AC-47.3] Public booking lifecycle: discover slots, send OTP, verify OTP, hold slot, and confirm booking")
    void shouldExecutePublicBookingFlowWithOtp() throws Exception {
        // 1. Discover public slots (AC-47.1)
        mockMvc.perform(get("/api/v1/public/slots")
                        .header("Host", "clinica-irma.medika.com")
                        .param("branchId", "sede-norte")
                        .param("date", "2026-11-17")
                        .param("durationMinutes", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].providerId", is("dr-alejandro-vega")))
                .andExpect(jsonPath("$[0].localStartTime", is("09:00:00")));

        // 2. Send OTP to patient (AC-47.2)
        String otpSendPayload = """
                {
                  "contact": "+52 55 9988 1122",
                  "patientName": "Valeria Santos"
                }
                """;

        mockMvc.perform(post("/api/v1/public/auth/otp/send")
                        .header("Host", "clinica-irma.medika.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(otpSendPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SENT")))
                .andExpect(jsonPath("$.expiresInSeconds", is(300)));

        // 3. Verify OTP code and receive JWT Bearer token (AC-47.3)
        String otpVerifyPayload = """
                {
                  "contact": "+52 55 9988 1122",
                  "code": "123456",
                  "patientName": "Valeria Santos"
                }
                """;

        MvcResult verifyResult = mockMvc.perform(post("/api/v1/public/auth/otp/verify")
                        .header("Host", "clinica-irma.medika.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(otpVerifyPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", startsWith("Bearer eyJ")))
                .andExpect(jsonPath("$.patientId", startsWith("pat-")))
                .andReturn();

        JsonNode verifyJson = objectMapper.readTree(verifyResult.getResponse().getContentAsString());
        String jwtToken = verifyJson.get("token").asText();
        String patientId = verifyJson.get("patientId").asText();

        // 4. Acquire 300s optimistic hold on slot
        String holdPayload = """
                {
                  "branchId": "sede-norte",
                  "doctorId": "dr-alejandro-vega",
                  "startTime": "2026-11-17T09:00:00Z",
                  "endTime": "2026-11-17T09:30:00Z",
                  "turnstileToken": "cf-turnstile-valid-token"
                }
                """;

        MvcResult holdResult = mockMvc.perform(post("/api/v1/slots/hold")
                        .header("Host", "clinica-irma.medika.com")
                        .header("X-Forwarded-For", "198.51.100.88")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(holdPayload))
                .andExpect(status().isCreated())
                .andReturn();

        String holdToken = objectMapper.readTree(holdResult.getResponse().getContentAsString()).get("holdToken").asText();

        // 5. Confirm self-service booking (AC-47.3)
        String confirmPayload = String.format("""
                {
                  "branchId": "sede-norte",
                  "roomId": "room-cardio-1",
                  "doctorId": "dr-alejandro-vega",
                  "patientId": "%s",
                  "patientName": "Valeria Santos",
                  "patientContact": "+52 55 9988 1122",
                  "serviceName": "Consulta Cardiología",
                  "specialty": "Cardiology",
                  "holdToken": "%s",
                  "startTime": "2026-11-17T09:00:00Z",
                  "endTime": "2026-11-17T09:30:00Z"
                }
                """, patientId, holdToken);

        mockMvc.perform(post("/api/v1/public/bookings/confirm")
                        .header("Host", "clinica-irma.medika.com")
                        .header("Authorization", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("SCHEDULED")))
                .andExpect(jsonPath("$.patientId", is(patientId)))
                .andExpect(jsonPath("$.serviceName", is("Consulta Cardiología")))
                .andExpect(jsonPath("$.holdToken", is(holdToken)));
    }
}
