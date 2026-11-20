package com.medcare.scheduling;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * [AHC-8, AHC-45, AHC-46, AHC-47] JPA Entity for confirmed patient appointments and clinic flow tracking.
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

    @Column(name = "service_name", length = 255)
    private String serviceName;

    @Column(name = "specialty", length = 128)
    private String specialty;

    @Column(name = "hold_token", nullable = false, unique = true, length = 128)
    private String holdToken;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "checked_in_at")
    private Instant checkedInAt;

    @Column(name = "consultation_started_at")
    private Instant consultationStartedAt;

    @Column(name = "consultation_completed_at")
    private Instant consultationCompletedAt;

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
        this(
                id,
                tenantSlug,
                branchId,
                roomId,
                doctorId,
                patientId,
                patientName,
                patientContact,
                "Consulta General",
                "General",
                holdToken,
                startTime,
                endTime,
                status,
                null,
                null,
                null
        );
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
            String serviceName,
            String specialty,
            String holdToken,
            Instant startTime,
            Instant endTime,
            String status,
            Instant checkedInAt,
            Instant consultationStartedAt,
            Instant consultationCompletedAt
    ) {
        this.id = id;
        this.tenantSlug = tenantSlug;
        this.branchId = branchId;
        this.roomId = roomId;
        this.doctorId = doctorId;
        this.patientId = patientId;
        this.patientName = patientName;
        this.patientContact = patientContact;
        this.serviceName = serviceName;
        this.specialty = specialty;
        this.holdToken = holdToken;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.checkedInAt = checkedInAt;
        this.consultationStartedAt = consultationStartedAt;
        this.consultationCompletedAt = consultationCompletedAt;
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

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(String doctorId) {
        this.doctorId = doctorId;
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

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    public String getHoldToken() {
        return holdToken;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCheckedInAt() {
        return checkedInAt;
    }

    public void setCheckedInAt(Instant checkedInAt) {
        this.checkedInAt = checkedInAt;
    }

    public Instant getConsultationStartedAt() {
        return consultationStartedAt;
    }

    public void setConsultationStartedAt(Instant consultationStartedAt) {
        this.consultationStartedAt = consultationStartedAt;
    }

    public Instant getConsultationCompletedAt() {
        return consultationCompletedAt;
    }

    public void setConsultationCompletedAt(Instant consultationCompletedAt) {
        this.consultationCompletedAt = consultationCompletedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
