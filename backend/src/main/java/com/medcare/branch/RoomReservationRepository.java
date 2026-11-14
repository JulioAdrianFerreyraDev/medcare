package com.medcare.branch;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomReservationRepository extends JpaRepository<RoomReservationEntity, UUID> {

    @Query("""
            SELECT COUNT(r) > 0 FROM RoomReservationEntity r
            WHERE r.roomId = :roomId
              AND r.status <> 'CANCELLED'
              AND r.startTime < :endTime
              AND r.endTime > :startTime
            """)
    boolean existsOverlappingReservation(
            @Param("roomId") UUID roomId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );
}
