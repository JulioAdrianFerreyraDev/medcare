package com.medcare.branch;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "physical_rooms")
public class PhysicalRoomEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_slug", nullable = false, length = 63)
    private String tenantSlug;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "room_type", nullable = false, length = 64)
    private String roomType;

    @Column(name = "concurrency_capacity", nullable = false)
    private int concurrencyCapacity;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PhysicalRoomEntity() {
    }

    public PhysicalRoomEntity(
            UUID id,
            String tenantSlug,
            UUID branchId,
            String name,
            String roomType,
            int concurrencyCapacity
    ) {
        this.id = id;
        this.tenantSlug = tenantSlug;
        this.branchId = branchId;
        this.name = name;
        this.roomType = roomType;
        this.concurrencyCapacity = concurrencyCapacity;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getTenantSlug() {
        return tenantSlug;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public String getName() {
        return name;
    }

    public String getRoomType() {
        return roomType;
    }

    public int getConcurrencyCapacity() {
        return concurrencyCapacity;
    }
}
