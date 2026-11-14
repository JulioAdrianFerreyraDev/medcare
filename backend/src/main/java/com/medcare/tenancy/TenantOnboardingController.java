package com.medcare.tenancy;

import com.medcare.tenancy.BrandOnboardingService.BrandOnboardingCommand;
import com.medcare.tenancy.BrandOnboardingService.BrandOnboardingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenants")
@Tag(name = "Tenancy & Brand Onboarding")
public class TenantOnboardingController {

    private final BrandOnboardingService onboardingService;

    public TenantOnboardingController(BrandOnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @PostMapping
    @Operation(
            summary = "Register a new medical brand tenant and provision its exclusive subdomain",
            description = "[AHC-3 AC-1 & AC-2] Validates slug format and reserved system words, returns 409 Conflict with 3 suggestions on collision, or provisions the tenant and dispatches an activation email."
    )
    public ResponseEntity<BrandOnboardingResponse> onboardBrand(@Valid @RequestBody TenantOnboardingRequest request) {
        BrandOnboardingResponse response = onboardingService.onboardBrand(new BrandOnboardingCommand(
                request.legalBusinessName(),
                request.contactEmail(),
                request.contactPhone(),
                request.slug()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/current")
    @Operation(
            summary = "Resolve the active brand tenant from the Host subdomain or X-Tenant-Slug header",
            description = "[AHC-3 AC-3] Returns the tenant context resolved by TenantInterceptor."
    )
    public ResponseEntity<BrandOnboardingResponse> getCurrentTenant() {
        return ResponseEntity.ok(onboardingService.getCurrentTenant());
    }

    public record TenantOnboardingRequest(
            @NotBlank String legalBusinessName,
            @NotBlank @Email String contactEmail,
            @NotBlank String contactPhone,
            @NotBlank String slug
    ) {}
}
