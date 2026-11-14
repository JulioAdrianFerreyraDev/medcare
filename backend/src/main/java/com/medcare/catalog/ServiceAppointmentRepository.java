package com.medcare.catalog;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServiceAppointmentRepository extends JpaRepository<ServiceAppointmentEntity, UUID> {

    List<ServiceAppointmentEntity> findAllByProviderId(String providerId);

    @Query("""
            SELECT COUNT(a) > 0 FROM ServiceAppointmentEntity a
            WHERE a.providerId = :providerId
              AND a.status <> 'CANCELLED'
              AND a.startTime < :endTime
              AND a.endTime > :startTime
            """)
    boolean existsOverlappingAppointment(
            @Param("providerId") String providerId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );
}
