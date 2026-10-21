package com.concurrency.locking.distributed.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

/**
 * <h1>DISTRIBUTED LOCK: RAW REDIS ATOMIC SETNX + LUA SCRIPT</h1>
 *
 * <h2>Interview Problem: "Why can't you just use `SETNX key val` and then `EXPIRE key 30`?"</h2>
 * <ul>
 *   <li><b>Non-Atomic Failure:</b> If your application crashes or network fails between {@code SETNX} and {@code EXPIRE},
 *       the key has NO TTL! It stays forever, causing a permanent system-wide deadlock!</li>
 *   <li><b>Fix:</b> Use atomic {@code SET key val NX PX 30000} (Available since Redis 2.6.12).</li>
 * </ul>
 *
 * <h2>Interview Problem: "Why can't you just call `DEL key` to release the lock?"</h2>
 * <ul>
 *   <li><b>The Premature Expiration Bug (Fatal Production Bug):</b>
 *       <ol>
 *         <li>Client 1 acquires lock with 10s TTL.</li>
 *         <li>Client 1 encounters a 12s stop-the-world JVM GC pause or slow I/O.</li>
 *         <li>At 10s, Redis expires Client 1's lock.</li>
 *         <li>Client 2 acquires the lock with its own 10s TTL.</li>
 *         <li>Client 1 wakes up from GC pause, finishes work, and blindly calls {@code DEL key}!</li>
 *         <li><b>Client 1 just deleted Client 2's lock!</b> Now Client 3 can also acquire the lock while Client 2 is still working!</li>
 *       </ol>
 *   </li>
 *   <li><b>The Solution: Atomic Lua Script Verification:</b>
 *       Only delete the lock IF the value stored matches this client's unique UUID!
 *   </li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomLuaRedisLock {

    private final StringRedisTemplate redisTemplate;

    // Lua script: checks if value matches before deletion
    private static final String RELEASE_LOCK_LUA_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "    return redis.call('del', KEYS[1]) " +
            "else " +
            "    return 0 " +
            "end";

    private final DefaultRedisScript<Long> releaseScript =
            new DefaultRedisScript<>(RELEASE_LOCK_LUA_SCRIPT, Long.class);

    /**
     * Attempts to acquire an atomic distributed lock.
     *
     * @param lockKey resource key
     * @param lockValue unique client/thread token (e.g. UUID + threadId)
     * @param ttl lease duration
     * @return true if acquired
     */
    public boolean acquireLock(String lockKey, String lockValue, Duration ttl) {
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, ttl);
        return Boolean.TRUE.equals(acquired);
    }

    /**
     * Atomically releases the distributed lock using a Lua script.
     * Prevents deleting another client's lock if TTL expired prematurely.
     */
    public boolean releaseLock(String lockKey, String lockValue) {
        Long result = redisTemplate.execute(
                releaseScript,
                Collections.singletonList(lockKey),
                lockValue
        );
        boolean released = result != null && result == 1L;
        if (!released) {
            log.warn("Lock release failed or lock already expired for key: {}", lockKey);
        }
        return released;
    }
}
