package com.medcare.tenancy;

import com.medcare.tenancy.BrandOnboardingService.BrandOnboardingCommand;
import com.medcare.tenancy.BrandOnboardingService.BrandOnboardingResponse;
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
public class TenantOnboardingController {

    private final BrandOnboardingService onboardingService;

    public TenantOnboardingController(BrandOnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @PostMapping
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
