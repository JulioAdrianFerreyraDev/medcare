package com.medcare.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "service_appointments")
public class ServiceAppointmentEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_slug", nullable = false, length = 63)
    private String tenantSlug;

    @Column(name = "provider_id", nullable = false, length = 128)
    private String providerId;

    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Column(name = "service_name_snapshot", nullable = false)
    private String serviceNameSnapshot;

    @Column(name = "patient_name", nullable = false)
    private String patientName;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "billed_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal billedFee;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ServiceAppointmentEntity() {
    }

    public ServiceAppointmentEntity(
            UUID id,
            String tenantSlug,
            String providerId,
            UUID serviceId,
            String serviceNameSnapshot,
            String patientName,
            Instant startTime,
            Instant endTime,
            int durationMinutes,
            BigDecimal billedFee,
            String status
    ) {
        this.id = id;
        this.tenantSlug = tenantSlug;
        this.providerId = providerId;
        this.serviceId = serviceId;
        this.serviceNameSnapshot = serviceNameSnapshot;
        this.patientName = patientName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationMinutes = durationMinutes;
        this.billedFee = billedFee;
        this.status = status;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getProviderId() {
        return providerId;
    }

    public UUID getServiceId() {
        return serviceId;
    }

    public String getServiceNameSnapshot() {
        return serviceNameSnapshot;
    }

    public String getPatientName() {
        return patientName;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public BigDecimal getBilledFee() {
        return billedFee;
    }

    public String getStatus() {
        return status;
    }
}
