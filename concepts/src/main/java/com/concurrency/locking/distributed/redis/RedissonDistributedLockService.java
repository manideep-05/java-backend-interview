package com.concurrency.locking.distributed.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * <h1>DISTRIBUTED LOCK: REDISSON (Production Standard)</h1>
 *
 * <h2>Why Redisson Over Homemade Redis Locks?</h2>
 * <ol>
 *   <li><b>Reentrancy Support:</b> Redisson stores locks as Redis Hashes:
 *       {@code HSET lockKey clientUUID:threadId 1}.
 *       If the same thread re-enters, it increments the hash value (recursion count).</li>
 *   <li><b>Watchdog Auto-Renewal (Lease Extension):</b>
 *       If no explicit lease time is provided, Redisson sets a 30-second TTL and spins up a background timer
 *       that renews the lease every 10 seconds (leaseTime / 3) as long as the holding thread is alive.
 *       If the node crashes, the watchdog stops and Redis safely cleans up after 30s!</li>
 *   <li><b>Pub/Sub Notification (No CPU Spin):</b>
 *       When a lock is released, Redisson publishes a message to a Redis channel.
 *       Waiting nodes listen via Redis Pub/Sub instead of hammering the Redis server with tight polling loops.</li>
 *   <li><b>FairLock Support:</b> Redisson uses Redis Sorted Sets and Lists to maintain a strict FIFO queue for waiting distributed nodes.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedissonDistributedLockService {

    private final RedissonClient redissonClient;

    /**
     * Executes an operation with a distributed lock using Redisson's Watchdog.
     *
     * @param lockKey resource identifier
     * @param waitTime maximum time to wait for lock acquisition
     * @param task business logic to execute
     */
    public <T> T executeWithLock(String lockKey, long waitTime, TimeUnit timeUnit, Supplier<T> task) {
        RLock lock = redissonClient.getLock(lockKey);
        boolean isAcquired = false;

        try {
            // Note: If leaseTime is omitted (-1), Redisson activates the Watchdog!
            isAcquired = lock.tryLock(waitTime, -1, timeUnit);
            if (!isAcquired) {
                log.warn("Could not acquire distributed lock for key: {} within {} {}", lockKey, waitTime, timeUnit);
                throw new IllegalStateException("Could not acquire lock for resource: " + lockKey);
            }

            log.info("Distributed lock acquired for key: {}, executing business logic...", lockKey);
            return task.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrupted while waiting for distributed lock", e);
        } finally {
            // Only unlock if current thread holds this lock
            if (isAcquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("Distributed lock released for key: {}", lockKey);
            }
        }
    }

    /**
     * Fair distributed lock (FIFO order across different cluster instances).
     */
    public <T> T executeWithFairLock(String lockKey, long waitTime, TimeUnit timeUnit, Supplier<T> task) {
        RLock fairLock = redissonClient.getFairLock(lockKey);
        boolean isAcquired = false;

        try {
            isAcquired = fairLock.tryLock(waitTime, -1, timeUnit);
            if (!isAcquired) {
                throw new IllegalStateException("Could not acquire fair lock: " + lockKey);
            }
            return task.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for fair lock", e);
        } finally {
            if (isAcquired && fairLock.isHeldByCurrentThread()) {
                fairLock.unlock();
            }
        }
    }
}
