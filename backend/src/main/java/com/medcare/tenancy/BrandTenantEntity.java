package com.medcare.tenancy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "brand_tenants")
public class BrandTenantEntity {

    @Id
    private UUID id;

    @Column(name = "legal_business_name", nullable = false)
    private String legalBusinessName;

    @Column(name = "contact_email", nullable = false)
    private String contactEmail;

    @Column(name = "contact_phone", nullable = false)
    private String contactPhone;

    @Column(name = "slug", nullable = false, unique = true, length = 63)
    private String slug;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "activation_email_dispatched", nullable = false)
    private boolean activationEmailDispatched;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected BrandTenantEntity() {
    }

    public BrandTenantEntity(
            UUID id,
            String legalBusinessName,
            String contactEmail,
            String contactPhone,
            String slug,
            String status,
            boolean activationEmailDispatched
    ) {
        this.id = id;
        this.legalBusinessName = legalBusinessName;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
        this.slug = slug;
        this.status = status;
        this.activationEmailDispatched = activationEmailDispatched;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getLegalBusinessName() {
        return legalBusinessName;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public String getSlug() {
        return slug;
    }

    public String getStatus() {
        return status;
    }

    public boolean isActivationEmailDispatched() {
        return activationEmailDispatched;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
