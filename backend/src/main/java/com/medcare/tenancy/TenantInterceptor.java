package com.medcare.tenancy;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * [AHC-11] Spring MVC HandlerInterceptor extracting the active brand slug from the HTTP Host header,
 * ignoring reserved subdomains, and clearing TenantContext in afterCompletion().
 */
@Component
public class TenantInterceptor implements HandlerInterceptor {

    private final SubdomainValidator subdomainValidator;

    public TenantInterceptor(SubdomainValidator subdomainValidator) {
        this.subdomainValidator = subdomainValidator;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String hostHeader = request.getHeader("Host");
        if (hostHeader != null && !hostHeader.isBlank()) {
            String hostWithoutPort = hostHeader.split(":")[0].trim().toLowerCase();
            boolean isIpv4 = hostWithoutPort.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$");
            if (!isIpv4) {
                String[] segments = hostWithoutPort.split("\\.");
                if (segments.length >= 2) {
                    String candidateSlug = segments[0];
                    if (!subdomainValidator.isReserved(candidateSlug) && !"localhost".equals(candidateSlug)) {
                        TenantContext.setCurrentTenant(candidateSlug);
                        return true;
                    }
                }
            }
        }

        String explicitTenant = request.getHeader("X-Tenant-Slug");
        if (explicitTenant != null && !explicitTenant.isBlank() && !subdomainValidator.isReserved(explicitTenant)) {
            TenantContext.setCurrentTenant(explicitTenant.trim().toLowerCase());
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantContext.clear();
    }
}
