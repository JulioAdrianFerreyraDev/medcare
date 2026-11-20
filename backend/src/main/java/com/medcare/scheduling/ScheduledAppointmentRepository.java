package com.medcare.scheduling;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScheduledAppointmentRepository extends JpaRepository<ScheduledAppointmentEntity, UUID> {

    Optional<ScheduledAppointmentEntity> findByIdAndTenantSlug(UUID id, String tenantSlug);

    List<ScheduledAppointmentEntity> findAllByTenantSlugAndDoctorId(String tenantSlug, String doctorId);

    List<ScheduledAppointmentEntity> findAllByTenantSlugAndBranchId(String tenantSlug, String branchId);

    @Query("""
            SELECT a FROM ScheduledAppointmentEntity a
            WHERE a.tenantSlug = :tenantSlug
              AND a.branchId = :branchId
              AND a.startTime >= :dayStart
              AND a.startTime < :dayEnd
            ORDER BY a.startTime ASC
            """)
    List<ScheduledAppointmentEntity> findAllForCalendar(
            @Param("tenantSlug") String tenantSlug,
            @Param("branchId") String branchId,
            @Param("dayStart") Instant dayStart,
            @Param("dayEnd") Instant dayEnd
    );

    @Query("""
            SELECT a FROM ScheduledAppointmentEntity a
            WHERE a.tenantSlug = :tenantSlug
              AND a.doctorId = :doctorId
              AND a.status NOT IN ('CANCELLED', 'REQUIRES_RESCHEDULING')
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
              AND a.status NOT IN ('CANCELLED', 'REQUIRES_RESCHEDULING')
              AND a.startTime < :endTime
              AND a.endTime > :startTime
            """)
    List<ScheduledAppointmentEntity> findOverlappingScheduledByRoom(
            @Param("tenantSlug") String tenantSlug,
            @Param("roomId") String roomId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );

    @Query("""
            SELECT a FROM ScheduledAppointmentEntity a
            WHERE a.tenantSlug = :tenantSlug
              AND a.doctorId = :doctorId
              AND a.id <> :excludeId
              AND a.status NOT IN ('CANCELLED', 'REQUIRES_RESCHEDULING')
              AND a.startTime < :endTime
              AND a.endTime > :startTime
            """)
    List<ScheduledAppointmentEntity> findOverlappingByDoctorExcludingId(
            @Param("tenantSlug") String tenantSlug,
            @Param("doctorId") String doctorId,
            @Param("excludeId") UUID excludeId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );

    @Query("""
            SELECT a FROM ScheduledAppointmentEntity a
            WHERE a.tenantSlug = :tenantSlug
              AND a.roomId = :roomId
              AND a.id <> :excludeId
              AND a.status NOT IN ('CANCELLED', 'REQUIRES_RESCHEDULING')
              AND a.startTime < :endTime
              AND a.endTime > :startTime
            """)
    List<ScheduledAppointmentEntity> findOverlappingByRoomExcludingId(
            @Param("tenantSlug") String tenantSlug,
            @Param("roomId") String roomId,
            @Param("excludeId") UUID excludeId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );
}
