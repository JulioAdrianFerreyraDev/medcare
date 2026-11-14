package com.medcare.availability;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
 * [AHC-30 / Story AHC-6] Spring Boot MockMvc Integration Tests for Provider Working Hours,
 * Meal Break Blocking, and Cross-Branch Shift Collision Prevention.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProviderShiftAndAvailabilityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProviderShiftRepository shiftRepository;

    @BeforeEach
    void cleanDatabase() {
        shiftRepository.deleteAll();
    }

    @Test
    @DisplayName("[AHC-6 AC-1] POST /api/v1/providers/{id}/shifts configures Tuesday split shifts (09:00-13:00 & 15:00-19:00) and GET available-slots keeps 13:00-15:00 blocked")
    void shouldConfigureSplitShiftsAndGenerateSlotsExcludingMealBreak() throws Exception {
        String shiftPayload = """
                {
                  "branchId": "sede-norte",
                  "branchName": "Sede Norte",
                  "dayOfWeek": "TUESDAY",
                  "slotIntervalMinutes": 60,
                  "windows": [
                    { "startTime": "09:00:00", "endTime": "13:00:00" },
                    { "startTime": "15:00:00", "endTime": "19:00:00" }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/providers/dr-alejandro-vega/shifts")
                        .header("Host", "clinica-irma.medika.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(shiftPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].startTime", is("09:00:00")))
                .andExpect(jsonPath("$[0].endTime", is("13:00:00")))
                .andExpect(jsonPath("$[1].startTime", is("15:00:00")))
                .andExpect(jsonPath("$[1].endTime", is("19:00:00")));

        // 2026-11-17 is a Tuesday
        mockMvc.perform(get("/api/v1/providers/dr-alejandro-vega/available-slots")
                        .header("Host", "clinica-irma.medika.com")
                        .param("branchId", "sede-norte")
                        .param("date", "2026-11-17")
                        .param("durationMinutes", "60"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(8)))
                .andExpect(jsonPath("$[*].localStartTime", hasItem("09:00:00")))
                .andExpect(jsonPath("$[*].localStartTime", hasItem("12:00:00")))
                .andExpect(jsonPath("$[*].localStartTime", hasItem("15:00:00")))
                .andExpect(jsonPath("$[*].localStartTime", hasItem("18:00:00")))
                .andExpect(jsonPath("$[*].localStartTime", not(hasItem("13:00:00"))))
                .andExpect(jsonPath("$[*].localStartTime", not(hasItem("14:00:00"))));
    }

    @Test
    @DisplayName("[AHC-6 AC-2] Rejects overlapping cross-branch shift on Tuesday (Sede Norte 09:00-14:00 vs Sede Sur 12:00-17:00) with HTTP 422 Unprocessable Entity")
    void shouldRejectCrossBranchShiftCollisionWith422() throws Exception {
        String sedeNorteShift = """
                {
                  "branchId": "sede-norte",
                  "branchName": "Sede Norte",
                  "dayOfWeek": "TUESDAY",
                  "slotIntervalMinutes": 30,
                  "windows": [
                    { "startTime": "09:00:00", "endTime": "14:00:00" }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/providers/dr-alejandro-vega/shifts")
                        .header("Host", "clinica-irma.medika.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sedeNorteShift))
                .andExpect(status().isCreated());

        String conflictingSedeSurShift = """
                {
                  "branchId": "sede-sur",
                  "branchName": "Sede Sur",
                  "dayOfWeek": "TUESDAY",
                  "slotIntervalMinutes": 30,
                  "windows": [
                    { "startTime": "12:00:00", "endTime": "17:00:00" }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/providers/dr-alejandro-vega/shifts")
                        .header("Host", "clinica-irma.medika.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(conflictingSedeSurShift))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title", is("Cross-Branch Shift Conflict")))
                .andExpect(jsonPath("$.detail", containsString("Sede Norte")))
                .andExpect(jsonPath("$.detail", containsString("Sede Sur")));
    }
}
