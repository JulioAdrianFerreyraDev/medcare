package com.medcare.catalog;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
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
 * [AHC-25] Spring Boot Integration Tests for Story AHC-5:
 * Provider Clinical Services & Pricing Catalog.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProviderClinicalServicesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClinicalServiceRepository serviceRepository;

    @Autowired
    private ServiceAppointmentRepository appointmentRepository;

    @BeforeEach
    void resetDatabase() {
        appointmentRepository.deleteAll();
        serviceRepository.deleteAll();
    }

    @Test
    @DisplayName("AC-1: POST /api/v1/providers/{id}/services should persist 'First-Time Cardiology Evaluation' (45 min, $800 MXN)")
    void shouldPersistProviderClinicalServiceUnderSpecialty() throws Exception {
        String providerId = UUID.randomUUID().toString();
        var payload = Map.of(
                "name", "First-Time Cardiology Evaluation",
                "specialty", "Cardiology",
                "durationMinutes", 45,
                "feeAmount", 800.00,
                "currency", "MXN"
        );

        mockMvc.perform(post("/api/v1/providers/" + providerId + "/services")
                        .with(user("dr-vega").roles("DOCTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.providerId").value(providerId))
                .andExpect(jsonPath("$.name").value("First-Time Cardiology Evaluation"))
                .andExpect(jsonPath("$.specialty").value("Cardiology"))
                .andExpect(jsonPath("$.durationMinutes").value(45))
                .andExpect(jsonPath("$.feeAmount").value(800.00))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("AC-2: Booking 'Minor Procedure' (60 min) at 10:00 AM reserves 10:00-11:00 AM and rejects overlapping 'Routine Checkup' (20 min) at 10:30 AM")
    void shouldReserveContinuous60MinSlotAndRejectPartialOverlap() throws Exception {
        String providerId = UUID.randomUUID().toString();
        String minorProcedureId = createService(providerId, "Minor Procedure", "General Surgery", 60, 1500.00);
        String routineCheckupId = createService(providerId, "Routine Checkup", "General Surgery", 20, 500.00);

        mockMvc.perform(post("/api/v1/providers/" + providerId + "/appointments")
                        .with(user("patient-1").roles("PATIENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "serviceId", minorProcedureId,
                                "patientName", "Carlos Rivera",
                                "startTime", "2026-11-16T10:00:00Z"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.startTime").value("2026-11-16T10:00:00Z"))
                .andExpect(jsonPath("$.endTime").value("2026-11-16T11:00:00Z"))
                .andExpect(jsonPath("$.durationMinutes").value(60));

        mockMvc.perform(post("/api/v1/providers/" + providerId + "/appointments")
                        .with(user("receptionist-1").roles("RECEPTIONIST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "serviceId", routineCheckupId,
                                "patientName", "Ana Gómez",
                                "startTime", "2026-11-16T10:30:00Z"
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Appointment Slot Overlap"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("AC-3: Soft-deactivating a service hides it from active booking catalog while preserving historical appointments & billing")
    void shouldHideDeactivatedServiceFromBookingCatalogAndPreserveHistoricalAppointment() throws Exception {
        String providerId = UUID.randomUUID().toString();
        String cardiologyServiceId = createService(
                providerId, "First-Time Cardiology Evaluation", "Cardiology", 45, 800.00
        );

        MvcResult bookingResult = mockMvc.perform(post("/api/v1/providers/" + providerId + "/appointments")
                        .with(user("patient-1").roles("PATIENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "serviceId", cardiologyServiceId,
                                "patientName", "Elena Torres",
                                "startTime", "2026-10-01T09:00:00Z"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        String appointmentId = objectMapper.readTree(bookingResult.getResponse().getContentAsString())
                .get("appointmentId").asText();

        mockMvc.perform(patch("/api/v1/providers/" + providerId + "/services/" + cardiologyServiceId + "/status")
                        .with(user("dr-vega").roles("DOCTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "INACTIVE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        mockMvc.perform(get("/api/v1/providers/" + providerId + "/services")
                        .param("activeOnly", "true")
                        .with(user("patient-2").roles("PATIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/api/v1/providers/" + providerId + "/appointments/" + appointmentId)
                        .with(user("dr-vega").roles("DOCTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointmentId").value(appointmentId))
                .andExpect(jsonPath("$.serviceId").value(cardiologyServiceId))
                .andExpect(jsonPath("$.serviceNameSnapshot").value("First-Time Cardiology Evaluation"))
                .andExpect(jsonPath("$.billedFee").value(800.00));
    }

    private String createService(String providerId, String name, String specialty, int duration, double fee) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/providers/" + providerId + "/services")
                        .with(user("dr-vega").roles("DOCTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", name,
                                "specialty", specialty,
                                "durationMinutes", duration,
                                "feeAmount", fee,
                                "currency", "MXN"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
}
