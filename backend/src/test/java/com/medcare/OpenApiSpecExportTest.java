package com.medcare;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Verifies Swagger UI & OpenAPI 3.1 configuration and exports the live specification
 * (/api/v1/api-docs) directly to ../frontend/openapi.json so that openapi-typescript
 * generates strictly synchronized TypeScript types.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiSpecExportTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should export complete OpenAPI 3.1 specification with security schemes, tags, and all domain endpoints to frontend/openapi.json")
    void shouldExportOpenApiSpecificationToFrontend() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        String rawJson = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode tree = objectMapper.readTree(rawJson);
        assertThat(tree.path("openapi").asText()).startsWith("3.");
        assertThat(tree.path("info").path("title").asText()).isEqualTo("Medika Clinical Platform API");
        assertThat(tree.path("info").path("version").asText()).isEqualTo("1.1.0");
        assertThat(tree.path("components").path("securitySchemes").has("bearerAuth")).isTrue();
        assertThat(tree.path("components").path("securitySchemes").has("tenantSlugHeader")).isTrue();

        JsonNode paths = tree.path("paths");
        assertThat(paths.has("/api/v1/tenants")).isTrue();
        assertThat(paths.has("/api/v1/branches")).isTrue();
        assertThat(paths.has("/api/v1/providers/{providerId}/services")).isTrue();
        assertThat(paths.has("/api/v1/providers/{providerId}/shifts")).isTrue();
        assertThat(paths.has("/api/v1/providers/{providerId}/available-slots")).isTrue();
        assertThat(paths.has("/api/v1/providers/{providerId}/time-off")).isTrue();
        assertThat(paths.has("/api/v1/providers/{providerId}/emergency-cancellations")).isTrue();
        assertThat(paths.has("/api/v1/slots/hold")).isTrue();
        assertThat(paths.has("/api/v1/appointments")).isTrue();

        String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(tree);
        Path targetPath = Paths.get("..", "frontend", "openapi.json").normalize().toAbsolutePath();
        if (Files.exists(targetPath.getParent())) {
            Files.writeString(targetPath, prettyJson + System.lineSeparator(), StandardCharsets.UTF_8);
        }
    }

    @Test
    @DisplayName("Should serve interactive Swagger UI at /api/v1/swagger-ui.html and /api/v1/swagger-ui/index.html")
    void shouldServeSwaggerUiEndpoints() throws Exception {
        MvcResult redirectResult = mockMvc.perform(get("/api/v1/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String redirectedUrl = redirectResult.getResponse().getRedirectedUrl();
        assertThat(redirectedUrl).isNotBlank();

        MvcResult uiResult = mockMvc.perform(get(redirectedUrl))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(uiResult.getResponse().getContentAsString()).contains("Swagger UI");
    }
}
