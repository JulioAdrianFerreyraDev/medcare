package com.medcare.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "clinical_services")
public class ClinicalServiceEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_slug", nullable = false, length = 63)
    private String tenantSlug;

    @Column(name = "provider_id", nullable = false, length = 128)
    private String providerId;

    @Column(name = "specialty", nullable = false, length = 128)
    private String specialty;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "fee_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal feeAmount;

    @Column(name = "currency", nullable = false, length = 16)
    private String currency;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ClinicalServiceEntity() {
    }

    public ClinicalServiceEntity(
            UUID id,
            String tenantSlug,
            String providerId,
            String specialty,
            String name,
            int durationMinutes,
            BigDecimal feeAmount,
            String currency,
            String status
    ) {
        this.id = id;
        this.tenantSlug = tenantSlug;
        this.providerId = providerId;
        this.specialty = specialty;
        this.name = name;
        this.durationMinutes = durationMinutes;
        this.feeAmount = feeAmount;
        this.currency = currency;
        this.status = status;
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

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    public void setFeeAmount(BigDecimal feeAmount) {
        this.feeAmount = feeAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
