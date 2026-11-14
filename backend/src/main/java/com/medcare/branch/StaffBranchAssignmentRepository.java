package com.medcare.branch;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffBranchAssignmentRepository extends JpaRepository<StaffBranchAssignmentEntity, UUID> {

    List<StaffBranchAssignmentEntity> findAllByTenantSlug(String tenantSlug);

    List<StaffBranchAssignmentEntity> findAllByTenantSlugAndStaffId(String tenantSlug, String staffId);

    List<StaffBranchAssignmentEntity> findAllByTenantSlugAndBranchIdAndRoleIgnoreCase(
            String tenantSlug,
            String branchId,
            String role
    );
}
