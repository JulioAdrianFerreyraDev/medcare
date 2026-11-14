package com.medcare.scheduling;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScheduledAppointmentRepository extends JpaRepository<ScheduledAppointmentEntity, UUID> {

    List<ScheduledAppointmentEntity> findAllByTenantSlugAndDoctorId(String tenantSlug, String doctorId);

    @Query("""
            SELECT a FROM ScheduledAppointmentEntity a
            WHERE a.tenantSlug = :tenantSlug
              AND a.doctorId = :doctorId
              AND a.status = 'SCHEDULED'
              AND a.startTime < :endTime
              AND a.endTime > :startTime
            """)
    List<ScheduledAppointmentEntity> findOverlappingScheduledByDoctor(
            @Param("tenantSlug") String tenantSlug,
            @Param("doctorId") String doctorId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );

    @Query("""
            SELECT a FROM ScheduledAppointmentEntity a
            WHERE a.tenantSlug = :tenantSlug
              AND a.roomId = :roomId
              AND a.status = 'SCHEDULED'
              AND a.startTime < :endTime
              AND a.endTime > :startTime
            """)
    List<ScheduledAppointmentEntity> findOverlappingScheduledByRoom(
            @Param("tenantSlug") String tenantSlug,
            @Param("roomId") String roomId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );
}
