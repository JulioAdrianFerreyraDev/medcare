package com.medcare.availability;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceptionistDashboardAlertRepository extends JpaRepository<ReceptionistDashboardAlertEntity, UUID> {

    List<ReceptionistDashboardAlertEntity> findAllByTenantSlugAndBranchIdOrderByCreatedAtDesc(
            String tenantSlug,
            String branchId
    );
}
