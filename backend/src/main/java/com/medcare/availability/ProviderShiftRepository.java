package com.medcare.availability;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProviderShiftRepository extends JpaRepository<ProviderShiftEntity, UUID> {

    List<ProviderShiftEntity> findAllByTenantSlugAndProviderId(String tenantSlug, String providerId);

    List<ProviderShiftEntity> findAllByTenantSlugAndProviderIdAndDayOfWeek(
            String tenantSlug,
            String providerId,
            String dayOfWeek
    );

    List<ProviderShiftEntity> findAllByTenantSlugAndProviderIdAndBranchIdAndDayOfWeek(
            String tenantSlug,
            String providerId,
            String branchId,
            String dayOfWeek
    );
}
