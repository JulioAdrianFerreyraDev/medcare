package com.medcare.branch;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicBranchRepository extends JpaRepository<ClinicBranchEntity, UUID> {

    boolean existsByIdAndTenantSlug(UUID id, String tenantSlug);

    List<ClinicBranchEntity> findAllByTenantSlug(String tenantSlug);
}
