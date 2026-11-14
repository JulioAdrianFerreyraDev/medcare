package com.medcare.tenancy;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandTenantRepository extends JpaRepository<BrandTenantEntity, UUID> {

    boolean existsBySlug(String slug);

    Optional<BrandTenantEntity> findBySlug(String slug);
}
