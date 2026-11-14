package com.medcare.tenancy;

import com.medcare.shared.exception.DomainException;
import java.util.List;
import org.springframework.http.HttpStatus;

/**
 * [AHC-10] Domain exception raised on subdomain slug collision (HTTP 409 Conflict)
 * carrying 3 available alternative slug suggestions.
 */
public class SubdomainCollisionException extends DomainException {

    private final List<String> suggestions;

    public SubdomainCollisionException(String slug, List<String> suggestions) {
        super(
                HttpStatus.CONFLICT,
                "Subdomain Collision",
                "The subdomain '" + slug + "' is already registered."
        );
        this.suggestions = List.copyOf(suggestions);
    }

    public List<String> getSuggestions() {
        return suggestions;
    }
}
