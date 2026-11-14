package com.medcare.branch;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "staff_branch_assignments")
public class StaffBranchAssignmentEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_slug", nullable = false, length = 63)
    private String tenantSlug;

    @Column(name = "staff_id", nullable = false, length = 128)
    private String staffId;

    @Column(name = "staff_name", nullable = false)
    private String staffName;

    @Column(name = "role", nullable = false, length = 32)
    private String role;

    @Column(name = "branch_id", nullable = false, length = 128)
    private String branchId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected StaffBranchAssignmentEntity() {
    }

    public StaffBranchAssignmentEntity(
            UUID id,
            String tenantSlug,
            String staffId,
            String staffName,
            String role,
            String branchId
    ) {
        this.id = id;
        this.tenantSlug = tenantSlug;
        this.staffId = staffId;
        this.staffName = staffName;
        this.role = role;
        this.branchId = branchId;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getTenantSlug() {
        return tenantSlug;
    }

    public String getStaffId() {
        return staffId;
    }

    public String getStaffName() {
        return staffName;
    }

    public String getRole() {
        return role;
    }

    public String getBranchId() {
        return branchId;
    }
}
