package com.medcare.availability;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleCancellationAuditLogRepository extends JpaRepository<ScheduleCancellationAuditLogEntity, UUID> {

    List<ScheduleCancellationAuditLogEntity> findAllByTenantSlugAndProviderIdOrderByRecordedAtDesc(
            String tenantSlug,
            String providerId
    );
}
