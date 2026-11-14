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
 * Exports the live SpringDoc OpenAPI 3.1 JSON specification (/api/v1/api-docs)
 * directly to ../frontend/openapi.json so that openapi-typescript can generate
 * strictly synchronized TypeScript types without manual DTO duplication.
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
    @DisplayName("Should export OpenAPI 3.1 specification from /api/v1/api-docs to frontend/openapi.json")
    void shouldExportOpenApiSpecificationToFrontend() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        String rawJson = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode tree = objectMapper.readTree(rawJson);
        assertThat(tree.path("openapi").asText()).startsWith("3.");
        assertThat(tree.path("paths").has("/api/v1/tenants")).isTrue();
        assertThat(tree.path("paths").has("/api/v1/branches")).isTrue();

        String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(tree);
        Path targetPath = Paths.get("..", "frontend", "openapi.json").normalize().toAbsolutePath();
        if (Files.exists(targetPath.getParent())) {
            Files.writeString(targetPath, prettyJson + System.lineSeparator(), StandardCharsets.UTF_8);
        }
    }
}
