package com.medcare.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Centralized OpenAPI 3.1 & Swagger UI configuration for the Medika Multi-Tenant Clinical Platform.
 */
@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH_SCHEME = "bearerAuth";
    public static final String TENANT_HEADER_SCHEME = "tenantSlugHeader";

    @Bean
    public OpenAPI medcareOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Medika Clinical Platform API")
                        .version("1.1.0")
                        .description("""
                                Multi-Tenant Healthcare SaaS REST API covering:
                                - **Tenancy & Brand Onboarding (AHC-3):** Automated subdomain provisioning, reserved slug validation, and collision suggestions.
                                - **Branches & Consultation Rooms (AHC-4):** Multi-branch clinic management, physical consultation rooms, `btree_gist` room reservation locks, and staff RBAC assignments.
                                - **Provider Services Catalog (AHC-5):** Clinical services pricing catalog, continuous duration slot allocation, and soft deactivation preserving historical records.
                                - **Provider Availability & Exceptions (AHC-6 & AHC-7):** Recurring weekly working shifts, meal break blocking, cross-branch collision prevention (`422`), planned leave blocking, emergency cancellations (`REQUIRES_RESCHEDULING`), receptionist alerts, and immutable audit logs.
                                - **Optimistic Slot Hold & Scheduling (AHC-8):** Unauthenticated 300s Redis slot holds (`lock:doctor:{id}:slot:{HHmm}`) with Turnstile verification & IP rate limiting (max 2 active holds/IP), and authenticated double-booking-free confirmation (`POST /api/v1/appointments`).
                                """)
                        .contact(new Contact()
                                .name("Advanta HealthConnect Architecture Team")
                                .email("architecture@medika.com")
                                .url("https://medika.com"))
                        .license(new License()
                                .name("Proprietary - Advanta Health Care")
                                .url("https://medika.com/terms")))
                .externalDocs(new ExternalDocumentation()
                        .description("Medika SRS & Architecture Documentation")
                        .url("https://julio-ferreyra-dev.atlassian.net/browse/AHC"))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local Development Server (Default)"),
                        new Server()
                                .url("http://clinica-irma.localhost:8080")
                                .description("Local Wildcard Subdomain Tenant (Clínica Irma)"),
                        new Server()
                                .url("https://api.medika.com")
                                .description("Production API Gateway")
                ))
                .tags(List.of(
                        new Tag()
                                .name("Tenancy & Brand Onboarding")
                                .description("[AHC-3] Brand tenant registration, subdomain slug validation, and tenant context resolution"),
                        new Tag()
                                .name("Branches & Consultation Rooms")
                                .description("[AHC-4] Multi-branch management, physical consultation rooms, room reservations, and staff branch assignments"),
                        new Tag()
                                .name("Provider Services Catalog")
                                .description("[AHC-5] Clinical services & pricing catalog, continuous slot block booking, and soft-deactivation"),
                        new Tag()
                                .name("Provider Availability & Exceptions")
                                .description("[AHC-6 & AHC-7] Weekly working shifts, meal breaks, cross-branch conflict detection, planned leave, emergency cancellations, and audit logs"),
                        new Tag()
                                .name("Optimistic Slot Hold & Scheduling")
                                .description("[AHC-8] Unauthenticated 300s Redis slot holds, IP rate limiting, and authenticated double-booking-free appointment confirmation")
                ))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH_SCHEME, new SecurityScheme()
                                .name(BEARER_AUTH_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Stateless JWT token obtained after login or WhatsApp/Email OTP verification"))
                        .addSecuritySchemes(TENANT_HEADER_SCHEME, new SecurityScheme()
                                .name("X-Tenant-Slug")
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .description("Explicit multi-tenant brand slug header (e.g. 'clinica-irma') when not invoking via wildcard subdomain Host")))
                .addSecurityItem(new SecurityRequirement()
                        .addList(TENANT_HEADER_SCHEME)
                        .addList(BEARER_AUTH_SCHEME));
    }
}
