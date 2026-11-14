package com.medcare.availability;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.medcare.scheduling.ScheduledAppointmentEntity;
import com.medcare.scheduling.ScheduledAppointmentRepository;
import java.time.Instant;
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

/**
 * [AHC-34 / Story AHC-7] Spring Boot MockMvc Integration Tests for Planned Leave Blocking,
 * Emergency Provider Cancellations, Receptionist Alerts, and Immutable Audit Trail.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProviderTimeOffAndEmergencyCancellationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProviderShiftRepository shiftRepository;

    @Autowired
    private ProviderTimeOffBlockRepository timeOffBlockRepository;

    @Autowired
    private ScheduleCancellationAuditLogRepository auditLogRepository;

    @Autowired
    private ReceptionistDashboardAlertRepository receptionistAlertRepository;

    @Autowired
    private ScheduledAppointmentRepository scheduledAppointmentRepository;

    @BeforeEach
    void cleanDatabase() {
        auditLogRepository.deleteAll();
        receptionistAlertRepository.deleteAll();
        timeOffBlockRepository.deleteAll();
        scheduledAppointmentRepository.deleteAll();
        shiftRepository.deleteAll();
    }

    @Test
    @DisplayName("[AHC-7 AC-1 & AC-3] Planned leave from Dec 20 to Dec 27 suppresses all slots in range and records immutable audit log")
    void shouldSuppressPublicAndInternalSlotsDuringPlannedLeaveAndRecordAuditLog() throws Exception {
        shiftRepository.save(new ProviderShiftEntity(
                UUID.randomUUID(),
                "clinica-irma",
                "dr-vega",
                "sede-norte",
                "Sede Norte",
                "TUESDAY",
                LocalTime.of(9, 0),
                LocalTime.of(13, 0),
                60
        ));

        String plannedLeavePayload = """
                {
                  "branchId": "sede-norte",
                  "startDateTime": "2026-12-20T00:00:00Z",
                  "endDateTime": "2026-12-27T23:59:59Z",
                  "reason": "Planned Annual Conference & Holiday Leave"
                }
                """;

        mockMvc.perform(post("/api/v1/providers/dr-vega/time-off")
                        .header("Host", "clinica-irma.medika.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(plannedLeavePayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.blockType", is("PLANNED_LEAVE")))
                .andExpect(jsonPath("$.affectedAppointmentsCount", is(0)))
                .andExpect(jsonPath("$.auditLogId", notNullValue()));

        // Tuesday Dec 22, 2026 falls within Dec 20 - Dec 27 -> all slots suppressed
        mockMvc.perform(get("/api/v1/providers/dr-vega/available-slots")
                        .header("Host", "clinica-irma.medika.com")
                        .param("branchId", "sede-norte")
                        .param("date", "2026-12-22"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // Verify immutable audit log entry
        mockMvc.perform(get("/api/v1/providers/dr-vega/cancellation-audit-logs")
                        .header("Host", "clinica-irma.medika.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].providerId", is("dr-vega")))
                .andExpect(jsonPath("$[0].actionType", is("PLANNED_LEAVE")))
                .andExpect(jsonPath("$[0].reason", is("Planned Annual Conference & Holiday Leave")))
                .andExpect(jsonPath("$[0].recordedAt", notNullValue()));
    }

    @Test
    @DisplayName("[AHC-7 AC-2 & AC-3] Emergency cancellation with 5 scheduled appointments transitions all 5 to REQUIRES_RESCHEDULING, creates HIGH receptionist alert, and writes audit log")
    void shouldTransitionFiveScheduledAppointmentsAndAlertReceptionistOnEmergencyCancellation() throws Exception {
        for (int i = 1; i <= 5; i++) {
            scheduledAppointmentRepository.save(new ScheduledAppointmentEntity(
                    UUID.randomUUID(),
                    "clinica-irma",
                    "sede-norte",
                    "room-cardio-1",
                    "dr-vega",
                    "patient-" + i,
                    "Patient " + i,
                    "+52 55 0000 000" + i,
                    "hold_token_emergency_" + i,
                    Instant.parse(String.format("2026-11-18T%02d:00:00Z", 8 + i)),
                    Instant.parse(String.format("2026-11-18T%02d:00:00Z", 9 + i)),
                    "SCHEDULED"
            ));
        }

        String emergencyPayload = """
                {
                  "branchId": "sede-norte",
                  "startDateTime": "2026-11-18T08:00:00Z",
                  "endDateTime": "2026-11-18T18:00:00Z",
                  "reason": "Urgent Hospital Surgical Call"
                }
                """;

        mockMvc.perform(post("/api/v1/providers/dr-vega/emergency-cancellations")
                        .header("Host", "clinica-irma.medika.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emergencyPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.blockType", is("EMERGENCY_CANCELLATION")))
                .andExpect(jsonPath("$.affectedAppointmentsCount", is(5)))
                .andExpect(jsonPath("$.rescheduledAppointmentIds", hasSize(5)))
                .andExpect(jsonPath("$.receptionistAlertId", notNullValue()))
                .andExpect(jsonPath("$.auditLogId", notNullValue()));

        // Verify all 5 appointments are now REQUIRES_RESCHEDULING
        mockMvc.perform(get("/api/v1/providers/dr-vega/scheduled-appointments")
                        .header("Host", "clinica-irma.medika.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[*].status", everyItem(is("REQUIRES_RESCHEDULING"))));

        // Verify high-priority alert appears on branch receptionist dashboard
        mockMvc.perform(get("/api/v1/branches/sede-norte/receptionist-alerts")
                        .header("Host", "clinica-irma.medika.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].severity", is("HIGH")))
                .andExpect(jsonPath("$[0].affectedAppointmentsCount", is(5)))
                .andExpect(jsonPath("$[0].status", is("OPEN")));

        // Verify immutable audit log
        mockMvc.perform(get("/api/v1/providers/dr-vega/cancellation-audit-logs")
                        .header("Host", "clinica-irma.medika.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].actionType", is("EMERGENCY_CANCELLATION")))
                .andExpect(jsonPath("$[0].reason", is("Urgent Hospital Surgical Call")))
                .andExpect(jsonPath("$[0].affectedAppointmentsCount", is(5)));
    }
}
