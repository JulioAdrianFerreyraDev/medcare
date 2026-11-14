package com.medcare.scheduling;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medcare.availability.ProviderShiftEntity;
import com.medcare.availability.ProviderShiftRepository;
import java.time.LocalTime;
import java.util.UUID;
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
 * [AHC-39 / Story AHC-8] Spring Boot MockMvc Integration Tests for Unauthenticated Optimistic Slot Hold,
 * Authenticated Confirmation with Hold Ownership, 300s Timeout Release, and Double-Booking Prevention.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OptimisticSlotHoldAndBookingIntegrationTest {

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

        shiftRepository.save(new ProviderShiftEntity(
                UUID.randomUUID(),
                "clinica-irma",
                "X",
                "sede-norte",
                "Sede Norte",
                "TUESDAY",
                LocalTime.of(9, 0),
                LocalTime.of(12, 0),
                60
        ));
    }

    @Test
    @DisplayName("[AHC-8 AC-1 & AC-2] Unauthenticated visitor acquires 300s Redis hold (lock:doctor:X:slot:1000) hiding 10:00 AM slot, then confirms with JWT + holdToken -> 201 Created")
    void shouldAcquireOptimisticHoldHideSlotAndConfirmWithJwtAndHoldToken() throws Exception {
        String holdPayload = """
                {
                  "branchId": "sede-norte",
                  "doctorId": "X",
                  "startTime": "2026-11-17T10:00:00Z",
                  "endTime": "2026-11-17T11:00:00Z",
                  "turnstileToken": "cf-turnstile-valid-token"
                }
                """;

        MvcResult holdResult = mockMvc.perform(post("/api/v1/slots/hold")
                        .header("Host", "clinica-irma.medika.com")
                        .header("X-Forwarded-For", "198.51.100.10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(holdPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.redisKey", is("lock:doctor:X:slot:1000")))
                .andExpect(jsonPath("$.holdToken", startsWith("hold_")))
                .andExpect(jsonPath("$.ttlSeconds", is(300)))
                .andReturn();

        JsonNode holdJson = objectMapper.readTree(holdResult.getResponse().getContentAsString());
        String holdToken = holdJson.get("holdToken").asText();

        // While held, the 10:00 AM slot is hidden from public availability (only 09:00 and 11:00 remain)
        mockMvc.perform(get("/api/v1/providers/X/available-slots")
                        .header("Host", "clinica-irma.medika.com")
                        .param("branchId", "sede-norte")
                        .param("date", "2026-11-17")
                        .param("durationMinutes", "60"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].localStartTime", not(hasItem("10:00:00"))));

        // Confirm appointment with valid JWT and matching holdToken -> 201 Created
        String confirmPayload = String.format("""
                {
                  "branchId": "sede-norte",
                  "roomId": "room-cardio-1",
                  "doctorId": "X",
                  "patientId": "pat-900",
                  "patientName": "Ana Sofía Martínez",
                  "patientContact": "+52 55 1234 9999",
                  "holdToken": "%s",
                  "startTime": "2026-11-17T10:00:00Z",
                  "endTime": "2026-11-17T11:00:00Z"
                }
                """, holdToken);

        mockMvc.perform(post("/api/v1/appointments")
                        .header("Host", "clinica-irma.medika.com")
                        .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.patient-otp-jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("SCHEDULED")))
                .andExpect(jsonPath("$.holdToken", is(holdToken)));
    }

    @Test
    @DisplayName("[AHC-8 AC-3] When unauthenticated visitor abandons checkout and 300s TTL expires, 10:00 AM slot returns to public availability")
    void shouldReturnSlotToPublicAvailabilityWhen300SecondHoldTtlExpires() throws Exception {
        String holdPayload = """
                {
                  "branchId": "sede-norte",
                  "doctorId": "X",
                  "startTime": "2026-11-17T10:00:00Z",
                  "endTime": "2026-11-17T11:00:00Z",
                  "turnstileToken": "cf-turnstile-valid-token"
                }
                """;

        mockMvc.perform(post("/api/v1/slots/hold")
                        .header("Host", "clinica-irma.medika.com")
                        .header("X-Forwarded-For", "198.51.100.25")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(holdPayload))
                .andExpect(status().isCreated());

        // Advance Redis clock by 301 seconds
        slotHoldStore.advanceTimeSeconds(301L);

        // 10:00 AM slot immediately returns to public availability (all 3 slots 09:00, 10:00, 11:00 available)
        mockMvc.perform(get("/api/v1/providers/X/available-slots")
                        .header("Host", "clinica-irma.medika.com")
                        .param("branchId", "sede-norte")
                        .param("date", "2026-11-17")
                        .param("durationMinutes", "60"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].localStartTime", hasItem("10:00:00")));
    }

    @Test
    @DisplayName("[AHC-8 AC-4] Concurrent/overlapping booking transaction for same doctor or room is rejected with HTTP 409 Conflict")
    void shouldRejectOverlappingBookingForSameDoctorOrRoomWith409Conflict() throws Exception {
        // First booking at 10:00 - 11:00
        SlotHoldStorePort.SlotHoldEntry hold1 = slotHoldStore.acquireHold(
                "clinica-irma",
                "sede-norte",
                "X",
                java.time.Instant.parse("2026-11-17T10:00:00Z"),
                java.time.Instant.parse("2026-11-17T11:00:00Z"),
                "198.51.100.31"
        );

        String firstBooking = String.format("""
                {
                  "branchId": "sede-norte",
                  "roomId": "room-cardio-1",
                  "doctorId": "X",
                  "patientId": "pat-1",
                  "patientName": "Patient One",
                  "patientContact": "p1@example.com",
                  "holdToken": "%s",
                  "startTime": "2026-11-17T10:00:00Z",
                  "endTime": "2026-11-17T11:00:00Z"
                }
                """, hold1.holdToken());

        mockMvc.perform(post("/api/v1/appointments")
                        .header("Host", "clinica-irma.medika.com")
                        .header("Authorization", "Bearer valid-jwt-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstBooking))
                .andExpect(status().isCreated());

        // Second overlapping transaction (10:30 - 11:30) for the same doctor or room -> 409 Conflict
        String secondOverlappingBooking = """
                {
                  "branchId": "sede-norte",
                  "roomId": "room-cardio-1",
                  "doctorId": "X",
                  "patientId": "pat-2",
                  "patientName": "Patient Two",
                  "patientContact": "p2@example.com",
                  "holdToken": "hold_concurrent_attempt",
                  "startTime": "2026-11-17T10:30:00Z",
                  "endTime": "2026-11-17T11:30:00Z"
                }
                """;

        mockMvc.perform(post("/api/v1/appointments")
                        .header("Host", "clinica-irma.medika.com")
                        .header("Authorization", "Bearer valid-jwt-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondOverlappingBooking))
                .andExpect(status().isConflict());
    }
}
