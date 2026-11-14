package com.medcare.tenancy;

import com.medcare.shared.exception.DomainException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [AHC-9 / AHC-10 / AHC-11] Application service handling Brand Onboarding,
 * reserved slug validation, collision suggestions, and tenant context lookup.
 */
@Service
public class BrandOnboardingService {

    private final BrandTenantRepository tenantRepository;
    private final SubdomainValidator subdomainValidator;
    private final SlugSuggestionGenerator slugSuggestionGenerator;
    private final BrandEmailNotificationPort emailNotificationPort;

    public BrandOnboardingService(
            BrandTenantRepository tenantRepository,
            SubdomainValidator subdomainValidator,
            SlugSuggestionGenerator slugSuggestionGenerator,
            BrandEmailNotificationPort emailNotificationPort
    ) {
        this.tenantRepository = tenantRepository;
        this.subdomainValidator = subdomainValidator;
        this.slugSuggestionGenerator = slugSuggestionGenerator;
        this.emailNotificationPort = emailNotificationPort;
    }

    @Transactional
    public BrandOnboardingResponse onboardBrand(BrandOnboardingCommand command) {
        String normalizedSlug = command.slug() != null ? command.slug().trim() : null;
        subdomainValidator.validateForOnboarding(normalizedSlug);

        if (tenantRepository.existsBySlug(normalizedSlug)) {
            List<String> suggestions = slugSuggestionGenerator.suggestThreeAlternatives(normalizedSlug);
            throw new SubdomainCollisionException(normalizedSlug, suggestions);
        }

        BrandTenantEntity entity = new BrandTenantEntity(
                UUID.randomUUID(),
                command.legalBusinessName(),
                command.contactEmail(),
                command.contactPhone(),
                normalizedSlug,
                "ACTIVE",
                true
        );
        BrandTenantEntity saved = tenantRepository.save(entity);
        String subdomainHost = saved.getSlug() + ".medika.com";
        emailNotificationPort.sendActivationEmail(
                saved.getContactEmail(),
                saved.getLegalBusinessName(),
                "https://" + subdomainHost
        );

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public BrandOnboardingResponse getCurrentTenant() {
        String activeSlug = TenantContext.getCurrentTenant();
        if (activeSlug == null || activeSlug.isBlank()) {
            throw new DomainException(
                    HttpStatus.BAD_REQUEST,
                    "Missing Tenant Context",
                    "No active brand tenant resolved from request Host header."
            );
        }
        BrandTenantEntity entity = tenantRepository.findBySlug(activeSlug)
                .orElseThrow(() -> new DomainException(
                        HttpStatus.NOT_FOUND,
                        "Tenant Not Found",
                        "No registered brand tenant found for subdomain: " + activeSlug
                ));
        return toResponse(entity);
    }

    private BrandOnboardingResponse toResponse(BrandTenantEntity entity) {
        return new BrandOnboardingResponse(
                entity.getId(),
                entity.getLegalBusinessName(),
                entity.getContactEmail(),
                entity.getContactPhone(),
                entity.getSlug(),
                entity.getSlug() + ".medika.com",
                entity.getStatus(),
                entity.isActivationEmailDispatched()
        );
    }

    public record BrandOnboardingCommand(
            String legalBusinessName,
            String contactEmail,
            String contactPhone,
            String slug
    ) {}

    public record BrandOnboardingResponse(
            UUID id,
            String legalBusinessName,
            String contactEmail,
            String contactPhone,
            String slug,
            String subdomainHost,
            String status,
            boolean activationEmailDispatched
    ) {}
}
