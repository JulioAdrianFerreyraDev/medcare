package com.medcare.scheduling;

import com.medcare.shared.exception.DomainException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * [AHC-8 AC-1, AC-2, AC-3] Thread-safe Redis-style optimistic lock store with 300-second TTL
 * and sliding-window IP rate limiting (max 2 active holds per IP).
 */
@Component
public class RedisSlotHoldStoreAdapter implements SlotHoldStorePort {

    public static final long HOLD_TTL_SECONDS = 300L;
    public static final int MAX_ACTIVE_HOLDS_PER_IP = 2;
    private static final DateTimeFormatter SLOT_KEY_FORMATTER =
            DateTimeFormatter.ofPattern("HHmm").withZone(ZoneOffset.UTC);

    private final Map<String, SlotHoldEntry> locksByRedisKey = new ConcurrentHashMap<>();
    private final Map<String, String> redisKeyByHoldToken = new ConcurrentHashMap<>();
    private final AtomicReference<Clock> clockRef = new AtomicReference<>(Clock.systemUTC());

    public void setClock(Clock clock) {
        this.clockRef.set(clock);
    }

    public void advanceTimeSeconds(long seconds) {
        Clock current = this.clockRef.get();
        this.clockRef.set(Clock.offset(current, Duration.ofSeconds(seconds)));
        evictExpired();
    }

    public void resetAll() {
        this.locksByRedisKey.clear();
        this.redisKeyByHoldToken.clear();
        this.clockRef.set(Clock.systemUTC());
    }

    @Override
    public synchronized SlotHoldEntry acquireHold(
            String tenantSlug,
            String branchId,
            String doctorId,
            Instant startTime,
            Instant endTime,
            String clientIp
    ) {
        evictExpired();

        String normalizedIp = (clientIp == null || clientIp.isBlank()) ? "127.0.0.1" : clientIp.trim();
        long activeHoldsForIp = locksByRedisKey.values().stream()
                .filter(entry -> entry.clientIp().equals(normalizedIp))
                .count();

        if (activeHoldsForIp >= MAX_ACTIVE_HOLDS_PER_IP) {
            throw new DomainException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Slot Hold Rate Limit Exceeded",
                    "Maximum of " + MAX_ACTIVE_HOLDS_PER_IP + " active slot holds allowed per IP address (" + normalizedIp + ")."
            );
        }

        if (isSlotHeld(tenantSlug, doctorId, startTime, endTime)) {
            throw new DomainException(
                    HttpStatus.CONFLICT,
                    "Slot Already Held",
                    "The requested slot for doctor '" + doctorId + "' at " + startTime + " is currently held by another visitor."
            );
        }

        String slotKeyTime = SLOT_KEY_FORMATTER.format(startTime);
        String redisKey = "lock:doctor:" + doctorId + ":slot:" + slotKeyTime;
        String holdToken = "hold_" + UUID.randomUUID();
        Instant now = Instant.now(clockRef.get());
        Instant expiresAt = now.plusSeconds(HOLD_TTL_SECONDS);

        SlotHoldEntry entry = new SlotHoldEntry(
                redisKey,
                holdToken,
                tenantSlug,
                branchId,
                doctorId,
                startTime,
                endTime,
                normalizedIp,
                expiresAt,
                HOLD_TTL_SECONDS
        );

        locksByRedisKey.put(redisKey, entry);
        redisKeyByHoldToken.put(holdToken, redisKey);
        return entry;
    }

    @Override
    public synchronized Optional<SlotHoldEntry> getActiveHoldByToken(String holdToken) {
        evictExpired();
        if (holdToken == null || holdToken.isBlank()) {
            return Optional.empty();
        }
        String redisKey = redisKeyByHoldToken.get(holdToken);
        if (redisKey == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(locksByRedisKey.get(redisKey));
    }

    @Override
    public synchronized boolean isSlotHeld(String tenantSlug, String doctorId, Instant startTime, Instant endTime) {
        evictExpired();
        return locksByRedisKey.values().stream()
                .anyMatch(entry -> entry.tenantSlug().equals(tenantSlug)
                        && entry.doctorId().equals(doctorId)
                        && entry.startTime().isBefore(endTime)
                        && entry.endTime().isAfter(startTime));
    }

    @Override
    public synchronized void releaseHold(String holdToken) {
        if (holdToken == null) {
            return;
        }
        String redisKey = redisKeyByHoldToken.remove(holdToken);
        if (redisKey != null) {
            locksByRedisKey.remove(redisKey);
        }
    }

    private void evictExpired() {
        Instant now = Instant.now(clockRef.get());
        locksByRedisKey.entrySet().removeIf(entry -> {
            if (!now.isBefore(entry.getValue().expiresAt())) {
                redisKeyByHoldToken.remove(entry.getValue().holdToken());
                return true;
            }
            return false;
        });
    }
}
