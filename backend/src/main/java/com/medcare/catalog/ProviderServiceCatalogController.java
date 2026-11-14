package com.medcare.catalog;

import com.medcare.catalog.ProviderServiceCatalogService.BookAppointmentCommand;
import com.medcare.catalog.ProviderServiceCatalogService.ClinicalServiceResponse;
import com.medcare.catalog.ProviderServiceCatalogService.CreateServiceCommand;
import com.medcare.catalog.ProviderServiceCatalogService.ServiceAppointmentResponse;
import com.medcare.tenancy.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/providers/{providerId}")
public class ProviderServiceCatalogController {

    private final ProviderServiceCatalogService catalogService;

    public ProviderServiceCatalogController(ProviderServiceCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @PostMapping("/services")
    public ResponseEntity<ClinicalServiceResponse> createService(
            @RequestHeader(value = "X-Tenant-Slug", required = false) String headerTenant,
            @PathVariable String providerId,
            @Valid @RequestBody CreateServiceRequest request
    ) {
        String tenantSlug = resolveTenant(headerTenant);
        ClinicalServiceResponse created = catalogService.createService(new CreateServiceCommand(
                tenantSlug,
                providerId,
                request.name(),
                request.specialty(),
                request.durationMinutes(),
                request.feeAmount(),
                request.currency()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/services/{serviceId}")
    public ResponseEntity<ClinicalServiceResponse> updateService(
            @RequestHeader(value = "X-Tenant-Slug", required = false) String headerTenant,
            @PathVariable String providerId,
            @PathVariable UUID serviceId,
            @Valid @RequestBody CreateServiceRequest request
    ) {
        String tenantSlug = resolveTenant(headerTenant);
        ClinicalServiceResponse updated = catalogService.updateService(serviceId, new CreateServiceCommand(
                tenantSlug,
                providerId,
                request.name(),
                request.specialty(),
                request.durationMinutes(),
                request.feeAmount(),
                request.currency()
        ));
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/services")
    public ResponseEntity<List<ClinicalServiceResponse>> listServices(
            @PathVariable String providerId,
            @RequestParam(defaultValue = "true") boolean activeOnly
    ) {
        return ResponseEntity.ok(catalogService.listServices(providerId, activeOnly));
    }

    @PatchMapping("/services/{serviceId}/status")
    public ResponseEntity<ClinicalServiceResponse> updateServiceStatus(
            @PathVariable String providerId,
            @PathVariable UUID serviceId,
            @Valid @RequestBody UpdateServiceStatusRequest request
    ) {
        return ResponseEntity.ok(catalogService.updateServiceStatus(providerId, serviceId, request.status()));
    }

    @GetMapping("/appointments")
    public ResponseEntity<List<ServiceAppointmentResponse>> listAppointments(
            @PathVariable String providerId
    ) {
        return ResponseEntity.ok(catalogService.listAppointments(providerId));
    }

    @PostMapping("/appointments")
    public ResponseEntity<ServiceAppointmentResponse> bookAppointment(
            @RequestHeader(value = "X-Tenant-Slug", required = false) String headerTenant,
            @PathVariable String providerId,
            @Valid @RequestBody BookServiceAppointmentRequest request
    ) {
        String tenantSlug = resolveTenant(headerTenant);
        ServiceAppointmentResponse booked = catalogService.bookServiceAppointment(new BookAppointmentCommand(
                tenantSlug,
                providerId,
                request.serviceId(),
                request.patientName(),
                request.startTime()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(booked);
    }

    @GetMapping("/appointments/{appointmentId}")
    public ResponseEntity<ServiceAppointmentResponse> getAppointment(
            @PathVariable String providerId,
            @PathVariable UUID appointmentId
    ) {
        return ResponseEntity.ok(catalogService.getHistoricalAppointment(appointmentId));
    }

    private String resolveTenant(String headerTenant) {
        if (headerTenant != null && !headerTenant.isBlank()) {
            return headerTenant.trim().toLowerCase();
        }
        return TenantContext.getCurrentTenantOrDefault("clinica-irma");
    }

    public record CreateServiceRequest(
            @NotBlank String name,
            @NotBlank String specialty,
            @Positive int durationMinutes,
            @NotNull @Positive BigDecimal feeAmount,
            @NotBlank String currency
    ) {}

    public record UpdateServiceStatusRequest(@NotBlank String status) {}

    public record BookServiceAppointmentRequest(
            @NotNull UUID serviceId,
            @NotBlank String patientName,
            @NotNull Instant startTime
    ) {}
}
