package com.medcare.branch;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "clinic_branches")
public class ClinicBranchEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_slug", nullable = false, length = 63)
    private String tenantSlug;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "address", nullable = false, length = 500)
    private String address;

    @Column(name = "timezone", nullable = false, length = 100)
    private String timezone;

    @Column(name = "contact_phone", nullable = false, length = 64)
    private String contactPhone;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ClinicBranchEntity() {
    }

    public ClinicBranchEntity(
            UUID id,
            String tenantSlug,
            String name,
            String address,
            String timezone,
            String contactPhone,
            boolean active
    ) {
        this.id = id;
        this.tenantSlug = tenantSlug;
        this.name = name;
        this.address = address;
        this.timezone = timezone;
        this.contactPhone = contactPhone;
        this.active = active;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getTenantSlug() {
        return tenantSlug;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getTimezone() {
        return timezone;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public boolean isActive() {
        return active;
    }
}
