package com.medcare.availability;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProviderTimeOffBlockRepository extends JpaRepository<ProviderTimeOffBlockEntity, UUID> {

    List<ProviderTimeOffBlockEntity> findAllByTenantSlugAndProviderId(String tenantSlug, String providerId);

    @Query("""
            SELECT b FROM ProviderTimeOffBlockEntity b
            WHERE b.tenantSlug = :tenantSlug
              AND b.providerId = :providerId
              AND b.startDateTime < :endTime
              AND b.endDateTime > :startTime
            """)
    List<ProviderTimeOffBlockEntity> findOverlappingBlocks(
            @Param("tenantSlug") String tenantSlug,
            @Param("providerId") String providerId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );
}
