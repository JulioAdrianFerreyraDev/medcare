package com.medcare.availability;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * [AHC-7 AC-3] Immutable audit trail entry recorded whenever a provider executes a schedule block or cancellation.
 */
@Entity
@Table(name = "schedule_cancellation_audit_logs")
public class ScheduleCancellationAuditLogEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_slug", nullable = false, length = 63)
    private String tenantSlug;

    @Column(name = "provider_id", nullable = false, length = 128)
    private String providerId;

    @Column(name = "block_id", nullable = false)
    private UUID blockId;

    @Column(name = "action_type", nullable = false, length = 32)
    private String actionType;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "affected_appointments_count", nullable = false)
    private int affectedAppointmentsCount;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected ScheduleCancellationAuditLogEntity() {
    }

    public ScheduleCancellationAuditLogEntity(
            UUID id,
            String tenantSlug,
            String providerId,
            UUID blockId,
            String actionType,
            String reason,
            int affectedAppointmentsCount,
            Instant recordedAt
    ) {
        this.id = id;
        this.tenantSlug = tenantSlug;
        this.providerId = providerId;
        this.blockId = blockId;
        this.actionType = actionType;
        this.reason = reason;
        this.affectedAppointmentsCount = affectedAppointmentsCount;
        this.recordedAt = recordedAt;
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

    public UUID getBlockId() {
        return blockId;
    }

    public String getActionType() {
        return actionType;
    }

    public String getReason() {
        return reason;
    }

    public int getAffectedAppointmentsCount() {
        return affectedAppointmentsCount;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
