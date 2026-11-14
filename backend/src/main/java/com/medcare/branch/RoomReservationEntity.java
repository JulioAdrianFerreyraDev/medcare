package com.medcare.branch;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "room_reservations")
public class RoomReservationEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_slug", nullable = false, length = 63)
    private String tenantSlug;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Column(name = "doctor_id", nullable = false)
    private UUID doctorId;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    protected RoomReservationEntity() {
    }

    public RoomReservationEntity(
            UUID id,
            String tenantSlug,
            UUID branchId,
            UUID roomId,
            UUID doctorId,
            Instant startTime,
            Instant endTime,
            String status
    ) {
        this.id = id;
        this.tenantSlug = tenantSlug;
        this.branchId = branchId;
        this.roomId = roomId;
        this.doctorId = doctorId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public UUID getRoomId() {
        return roomId;
    }

    public UUID getDoctorId() {
        return doctorId;
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
}
