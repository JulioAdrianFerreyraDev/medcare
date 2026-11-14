package com.medcare.tenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

/**
 * [AHC-13] Spring Boot Integration Tests for Story AHC-3:
 * Automated Subdomain Provisioning & Brand Onboarding.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TenantOnboardingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BrandTenantRepository tenantRepository;

    @BeforeEach
    void resetStore() {
        tenantRepository.deleteAll();
        TenantContext.clear();
    }

    @Test
    @DisplayName("AC-1: POST /api/v1/tenants should provision brand tenant and return 201 Created with subdomain routing")
    void shouldProvisionTenantAndReturn201Created() throws Exception {
        var payload = Map.of(
                "legalBusinessName", "Clínica Irma S.A. de C.V.",
                "contactEmail", "direccion@clinica-irma.com",
                "contactPhone", "+525512345678",
                "slug", "clinica-irma"
        );

        mockMvc.perform(post("/api/v1/tenants")
                        .with(user("brand-owner").roles("BRAND_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("clinica-irma"))
                .andExpect(jsonPath("$.subdomainHost").value("clinica-irma.medika.com"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.activationEmailDispatched").value(true));
    }

    @Test
    @DisplayName("AC-1: POST /api/v1/tenants with reserved subdomain 'admin' should return 422 ProblemDetail")
    void shouldRejectReservedSlugWithProblemDetail() throws Exception {
        var payload = Map.of(
                "legalBusinessName", "Admin Portal Test",
                "contactEmail", "admin@medika.com",
                "contactPhone", "+525500001111",
                "slug", "admin"
        );

        mockMvc.perform(post("/api/v1/tenants")
                        .with(user("brand-owner").roles("BRAND_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Reserved Subdomain"))
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    @DisplayName("AC-2: POST /api/v1/tenants with colliding slug 'clinica-irma' should return 409 Conflict and 3 slug suggestions")
    void shouldReturn409ConflictAndThreeAlternativeSuggestionsOnSlugCollision() throws Exception {
        var firstPayload = Map.of(
                "legalBusinessName", "Clínica Irma Original",
                "contactEmail", "original@clinica-irma.com",
                "contactPhone", "+525511112222",
                "slug", "clinica-irma"
        );
        mockMvc.perform(post("/api/v1/tenants")
                        .with(user("brand-owner").roles("BRAND_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstPayload)))
                .andExpect(status().isCreated());

        var collidingPayload = Map.of(
                "legalBusinessName", "Clínica Irma Segunda",
                "contactEmail", "segunda@clinica-irma.com",
                "contactPhone", "+525533334444",
                "slug", "clinica-irma"
        );
        mockMvc.perform(post("/api/v1/tenants")
                        .with(user("applicant-2").roles("BRAND_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(collidingPayload)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Subdomain Collision"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.suggestions", hasSize(3)))
                .andExpect(jsonPath("$.suggestions", containsInAnyOrder(
                        "clinica-irma-salud",
                        "clinica-irma-med",
                        "clinica-irma-clinica"
                )));
    }

    @Test
    @DisplayName("AC-3: Incoming HTTP request with Host 'clinica-irma.medika.com' should resolve TenantContext and isolate tenant records")
    void shouldResolveTenantFromHostHeaderAndIsolateQueries() throws Exception {
        tenantRepository.save(new BrandTenantEntity(
                UUID.randomUUID(), "Clínica Irma", "contacto@clinica-irma.com", "+525511112222", "clinica-irma", "ACTIVE", true
        ));
        tenantRepository.save(new BrandTenantEntity(
                UUID.randomUUID(), "Clínica Norte", "contacto@clinica-norte.com", "+525533334444", "clinica-norte", "ACTIVE", true
        ));

        mockMvc.perform(get("/api/v1/tenants/current")
                        .with(user("doctor-irma").roles("DOCTOR"))
                        .header("Host", "clinica-irma.medika.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("clinica-irma"))
                .andExpect(jsonPath("$.legalBusinessName").value("Clínica Irma"));

        assertThat(TenantContext.getCurrentTenant()).isNull();
    }
}
