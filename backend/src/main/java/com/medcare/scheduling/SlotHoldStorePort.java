package com.medcare.scheduling;

import java.time.Instant;
import java.util.Optional;

/**
 * [AHC-8 AC-1, AC-2, AC-3] Port representing Redis optimistic slot holds with 300-second TTL and IP rate limiting.
 */
public interface SlotHoldStorePort {

    record SlotHoldEntry(
            String redisKey,
            String holdToken,
            String tenantSlug,
            String branchId,
            String doctorId,
            Instant startTime,
            Instant endTime,
            String clientIp,
            Instant expiresAt,
            long ttlSeconds
    ) {
    }

    SlotHoldEntry acquireHold(
            String tenantSlug,
            String branchId,
            String doctorId,
            Instant startTime,
            Instant endTime,
            String clientIp
    );

    Optional<SlotHoldEntry> getActiveHoldByToken(String holdToken);

    boolean isSlotHeld(String tenantSlug, String doctorId, Instant startTime, Instant endTime);

    void releaseHold(String holdToken);
}
