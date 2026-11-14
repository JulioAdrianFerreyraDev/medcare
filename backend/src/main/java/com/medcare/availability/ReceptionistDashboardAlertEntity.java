package com.medcare.availability;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * [AHC-7 AC-2] JPA Entity for high-priority branch receptionist dashboard alerts triggered on emergency cancellations.
 */
@Entity
@Table(name = "receptionist_dashboard_alerts")
public class ReceptionistDashboardAlertEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_slug", nullable = false, length = 63)
    private String tenantSlug;

    @Column(name = "branch_id", nullable = false, length = 128)
    private String branchId;

    @Column(name = "provider_id", nullable = false, length = 128)
    private String providerId;

    @Column(name = "severity", nullable = false, length = 32)
    private String severity;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @Column(name = "affected_appointments_count", nullable = false)
    private int affectedAppointmentsCount;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ReceptionistDashboardAlertEntity() {
    }

    public ReceptionistDashboardAlertEntity(
            UUID id,
            String tenantSlug,
            String branchId,
            String providerId,
            String severity,
            String title,
            String message,
            int affectedAppointmentsCount,
            String status
    ) {
        this.id = id;
        this.tenantSlug = tenantSlug;
        this.branchId = branchId;
        this.providerId = providerId;
        this.severity = severity;
        this.title = title;
        this.message = message;
        this.affectedAppointmentsCount = affectedAppointmentsCount;
        this.status = status;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getTenantSlug() {
        return tenantSlug;
    }

    public String getBranchId() {
        return branchId;
    }

    public String getProviderId() {
        return providerId;
    }

    public String getSeverity() {
        return severity;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public int getAffectedAppointmentsCount() {
        return affectedAppointmentsCount;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
