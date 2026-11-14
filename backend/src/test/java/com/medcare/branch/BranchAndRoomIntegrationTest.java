package com.medcare.branch;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
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
 * [AHC-19] Spring Boot Integration Tests for Story AHC-4:
 * Multi-Branch & Physical Room Management.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BranchAndRoomIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClinicBranchRepository branchRepository;

    @Autowired
    private PhysicalRoomRepository roomRepository;

    @Autowired
    private RoomReservationRepository reservationRepository;

    @Autowired
    private StaffBranchAssignmentRepository staffAssignmentRepository;

    @BeforeEach
    void resetDatabase() {
        reservationRepository.deleteAll();
        roomRepository.deleteAll();
        staffAssignmentRepository.deleteAll();
        branchRepository.deleteAll();
    }

    @Test
    @DisplayName("AC-1: POST /api/v1/branches should persist a new clinic branch under the active brand tenant")
    void shouldPersistBranchUnderActiveBrandTenant() throws Exception {
        var branchRequest = Map.of(
                "name", "Sede Centro",
                "address", "Av. Paseo de la Reforma 222, CDMX",
                "timezone", "America/Mexico_City",
                "contactPhone", "+525511223344"
        );

        mockMvc.perform(post("/api/v1/branches")
                        .with(user("brand-admin").roles("BRAND_ADMIN"))
                        .header("X-Tenant-Slug", "clinica-irma")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(branchRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenantSlug").value("clinica-irma"))
                .andExpect(jsonPath("$.name").value("Sede Centro"))
                .andExpect(jsonPath("$.timezone").value("America/Mexico_City"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @DisplayName("AC-2: Physical consultation room should enforce capacity = 1 and reject overlapping room reservations with 409 Conflict")
    void shouldEnforceRoomCapacityOneAndRejectOverlappingRoomReservations() throws Exception {
        String branchId = createBranch("Sede Centro", "Av. Reforma 222");

        MvcResult roomResult = mockMvc.perform(post("/api/v1/branches/" + branchId + "/rooms")
                        .with(user("brand-admin").roles("BRAND_ADMIN"))
                        .header("X-Tenant-Slug", "clinica-irma")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Consultorio Dental 1",
                                "roomType", "DENTAL"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Consultorio Dental 1"))
                .andExpect(jsonPath("$.concurrencyCapacity").value(1))
                .andReturn();

        JsonNode roomJson = objectMapper.readTree(roomResult.getResponse().getContentAsString());
        String roomId = roomJson.get("id").asText();

        mockMvc.perform(post("/api/v1/branches/" + branchId + "/rooms/" + roomId + "/reservations")
                        .with(user("receptionist").roles("RECEPTIONIST"))
                        .header("X-Tenant-Slug", "clinica-irma")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "doctorId", UUID.randomUUID().toString(),
                                "startTime", "2026-11-16T10:00:00Z",
                                "endTime", "2026-11-16T11:00:00Z"
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/branches/" + branchId + "/rooms/" + roomId + "/reservations")
                        .with(user("receptionist").roles("RECEPTIONIST"))
                        .header("X-Tenant-Slug", "clinica-irma")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "doctorId", UUID.randomUUID().toString(),
                                "startTime", "2026-11-16T10:30:00Z",
                                "endTime", "2026-11-16T11:15:00Z"
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Physical Room Slot Conflict"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("AC-3: Doctor assigned to 'Sede Centro' and 'Sede Norte' appears in both matrices; Receptionist multi-branch assignment fails with 422")
    void shouldAllowMultiBranchDoctorVisibilityAndRejectMultiBranchReceptionistWith422() throws Exception {
        String sedeCentroId = createBranch("Sede Centro", "Av. Reforma 222");
        String sedeNorteId = createBranch("Sede Norte", "Av. Montevideo 500");
        String doctorId = UUID.randomUUID().toString();
        String receptionistId = UUID.randomUUID().toString();

        for (String branchId : List.of(sedeCentroId, sedeNorteId)) {
            mockMvc.perform(post("/api/v1/branches/staff-assignments")
                            .with(user("brand-admin").roles("BRAND_ADMIN"))
                            .header("X-Tenant-Slug", "clinica-irma")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "staffId", doctorId,
                                    "staffName", "Dr. Alejandro Vega",
                                    "role", "DOCTOR",
                                    "branchId", branchId
                            ))))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/v1/branches/" + sedeCentroId + "/scheduling-matrix")
                        .with(user("brand-admin").roles("BRAND_ADMIN"))
                        .header("X-Tenant-Slug", "clinica-irma"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providers", hasSize(1)))
                .andExpect(jsonPath("$.providers[*].staffId", hasItem(doctorId)));

        mockMvc.perform(get("/api/v1/branches/" + sedeNorteId + "/scheduling-matrix")
                        .with(user("brand-admin").roles("BRAND_ADMIN"))
                        .header("X-Tenant-Slug", "clinica-irma"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providers", hasSize(1)))
                .andExpect(jsonPath("$.providers[*].staffId", hasItem(doctorId)));

        mockMvc.perform(post("/api/v1/branches/staff-assignments")
                        .with(user("brand-admin").roles("BRAND_ADMIN"))
                        .header("X-Tenant-Slug", "clinica-irma")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "staffId", receptionistId,
                                "staffName", "Laura Méndez",
                                "role", "RECEPTIONIST",
                                "branchId", sedeCentroId
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/branches/staff-assignments")
                        .with(user("brand-admin").roles("BRAND_ADMIN"))
                        .header("X-Tenant-Slug", "clinica-irma")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "staffId", receptionistId,
                                "staffName", "Laura Méndez",
                                "role", "RECEPTIONIST",
                                "branchId", sedeNorteId
                        ))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Single-Branch Constraint Violation"))
                .andExpect(jsonPath("$.status").value(422));
    }

    private String createBranch(String name, String address) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/branches")
                        .with(user("brand-admin").roles("BRAND_ADMIN"))
                        .header("X-Tenant-Slug", "clinica-irma")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", name,
                                "address", address,
                                "timezone", "America/Mexico_City",
                                "contactPhone", "+525511223344"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
}
