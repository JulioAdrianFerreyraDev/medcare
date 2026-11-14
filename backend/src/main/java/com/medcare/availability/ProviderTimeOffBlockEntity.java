package com.medcare.availability;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * [AHC-7 AC-1, AC-2] JPA Entity for planned leave or emergency provider cancellation blocks.
 */
@Entity
@Table(name = "provider_time_off_blocks")
public class ProviderTimeOffBlockEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_slug", nullable = false, length = 63)
    private String tenantSlug;

    @Column(name = "provider_id", nullable = false, length = 128)
    private String providerId;

    @Column(name = "branch_id", nullable = false, length = 128)
    private String branchId;

    @Column(name = "block_type", nullable = false, length = 32)
    private String blockType;

    @Column(name = "start_date_time", nullable = false)
    private Instant startDateTime;

    @Column(name = "end_date_time", nullable = false)
    private Instant endDateTime;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "affected_appointments_count", nullable = false)
    private int affectedAppointmentsCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ProviderTimeOffBlockEntity() {
    }

    public ProviderTimeOffBlockEntity(
            UUID id,
            String tenantSlug,
            String providerId,
            String branchId,
            String blockType,
            Instant startDateTime,
            Instant endDateTime,
            String reason,
            int affectedAppointmentsCount
    ) {
        this.id = id;
        this.tenantSlug = tenantSlug;
        this.providerId = providerId;
        this.branchId = branchId;
        this.blockType = blockType;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.reason = reason;
        this.affectedAppointmentsCount = affectedAppointmentsCount;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getTenantSlug() {
        return tenantSlug;
    }

    public String getProviderId() {
        return providerId;
    }

    public String getBranchId() {
        return branchId;
    }

    public String getBlockType() {
        return blockType;
    }

    public Instant getStartDateTime() {
        return startDateTime;
    }

    public Instant getEndDateTime() {
        return endDateTime;
    }

    public String getReason() {
        return reason;
    }

    public int getAffectedAppointmentsCount() {
        return affectedAppointmentsCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
