package com.medcare.tenancy;

/**
 * [AHC-11] Thread-safe Multi-Tenant Context Holder (Invariant 1: FR-TEN-001 to 004).
 * Cleared strictly in TenantInterceptor.afterCompletion() to prevent thread contamination.
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setCurrentTenant(String tenantSlug) {
        CURRENT_TENANT.set(tenantSlug);
    }

    public static String getCurrentTenant() {
        return CURRENT_TENANT.get();
    }

    public static String getCurrentTenantOrDefault(String fallback) {
        String current = CURRENT_TENANT.get();
        return (current != null && !current.isBlank()) ? current : fallback;
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
