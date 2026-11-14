package com.medcare.tenancy;

import com.medcare.shared.exception.DomainException;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * [AHC-9] Subdomain slug validator enforcing lowercase alphanumeric/hyphen regex
 * and rejecting system-reserved subdomains.
 */
@Component
public class SubdomainValidator {

    private static final Set<String> RESERVED_SLUGS = Set.of(
            "api", "admin", "www", "app", "auth", "billing", "root", "support"
    );

    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9](?:[a-z0-9-]{1,61}[a-z0-9])$");

    public boolean isReserved(String slug) {
        return slug != null && RESERVED_SLUGS.contains(slug.trim().toLowerCase());
    }

    public void validateForOnboarding(String slug) {
        if (slug == null || !SLUG_PATTERN.matcher(slug).matches()) {
            throw new DomainException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Invalid Subdomain Format",
                    "Invalid subdomain slug format: must be 3-63 lowercase alphanumeric characters or hyphens."
            );
        }
        if (isReserved(slug)) {
            throw new DomainException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Reserved Subdomain",
                    "Subdomain '" + slug + "' is on the reserved system list."
            );
        }
    }
}
