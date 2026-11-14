package com.medcare.tenancy;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * [AHC-10] Collision suggestion engine generating 3 available alternative subdomain slugs.
 */
@Component
public class SlugSuggestionGenerator {

    private static final List<String> CANDIDATE_SUFFIXES = List.of(
            "-salud", "-med", "-clinica", "-care", "-centro", "-1", "-2", "-3"
    );

    private final BrandTenantRepository tenantRepository;

    public SlugSuggestionGenerator(BrandTenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public List<String> suggestThreeAlternatives(String baseSlug) {
        List<String> available = new ArrayList<>(3);
        for (String suffix : CANDIDATE_SUFFIXES) {
            String candidate = baseSlug + suffix;
            if (!tenantRepository.existsBySlug(candidate)) {
                available.add(candidate);
                if (available.size() == 3) {
                    break;
                }
            }
        }
        return available;
    }
}
