package com.medcare.scheduling;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * [AHC-8 AC-2, AC-4 & AHC-7 AC-2] JPA Entity for confirmed patient appointments.
 */
@Entity
@Table(name = "scheduled_appointments")
public class ScheduledAppointmentEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_slug", nullable = false, length = 63)
    private String tenantSlug;

    @Column(name = "branch_id", nullable = false, length = 128)
    private String branchId;

    @Column(name = "room_id", nullable = false, length = 128)
    private String roomId;

    @Column(name = "doctor_id", nullable = false, length = 128)
    private String doctorId;

    @Column(name = "patient_id", nullable = false, length = 128)
    private String patientId;

    @Column(name = "patient_name", nullable = false, length = 255)
    private String patientName;

    @Column(name = "patient_contact", nullable = false, length = 255)
    private String patientContact;

    @Column(name = "hold_token", nullable = false, unique = true, length = 128)
    private String holdToken;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ScheduledAppointmentEntity() {
    }

    public ScheduledAppointmentEntity(
            UUID id,
            String tenantSlug,
            String branchId,
            String roomId,
            String doctorId,
            String patientId,
            String patientName,
            String patientContact,
            String holdToken,
            Instant startTime,
            Instant endTime,
            String status
    ) {
        this.id = id;
        this.tenantSlug = tenantSlug;
        this.branchId = branchId;
        this.roomId = roomId;
        this.doctorId = doctorId;
        this.patientId = patientId;
        this.patientName = patientName;
        this.patientContact = patientContact;
        this.holdToken = holdToken;
        this.startTime = startTime;
        this.endTime = endTime;
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

    public String getRoomId() {
        return roomId;
    }

    public String getDoctorId() {
        return doctorId;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getPatientContact() {
        return patientContact;
    }

    public String getHoldToken() {
        return holdToken;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
