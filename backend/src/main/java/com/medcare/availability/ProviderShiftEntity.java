package com.medcare.availability;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

/**
 * [AHC-6 AC-1, AC-2] JPA Entity representing a recurring weekly working shift window for a provider at a branch.
 */
@Entity
@Table(name = "provider_shifts")
public class ProviderShiftEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_slug", nullable = false, length = 63)
    private String tenantSlug;

    @Column(name = "provider_id", nullable = false, length = 128)
    private String providerId;

    @Column(name = "branch_id", nullable = false, length = 128)
    private String branchId;

    @Column(name = "branch_name", nullable = false, length = 255)
    private String branchName;

    @Column(name = "day_of_week", nullable = false, length = 16)
    private String dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "slot_interval_minutes", nullable = false)
    private int slotIntervalMinutes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ProviderShiftEntity() {
    }

    public ProviderShiftEntity(
            UUID id,
            String tenantSlug,
            String providerId,
            String branchId,
            String branchName,
            String dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            int slotIntervalMinutes
    ) {
        this.id = id;
        this.tenantSlug = tenantSlug;
        this.providerId = providerId;
        this.branchId = branchId;
        this.branchName = branchName;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.slotIntervalMinutes = slotIntervalMinutes;
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

    public String getBranchName() {
        return branchName;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public int getSlotIntervalMinutes() {
        return slotIntervalMinutes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
