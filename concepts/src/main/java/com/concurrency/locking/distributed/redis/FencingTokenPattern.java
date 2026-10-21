package com.concurrency.locking.distributed.redis;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * <h1>FENCING TOKEN PATTERN (The Martin Kleppmann Dilemma)</h1>
 *
 * <h2>The Deep SDE-2 Interview Trap:</h2>
 * <p><b>Interviewer:</b> "Can a Redis distributed lock guarantee 100% data correctness under all failure conditions?"</p>
 * <p><b>Correct Answer:</b> "NO! If the holding process suffers a Stop-The-World (STW) JVM Garbage Collection pause
 * or network delay, its lock TTL expires in Redis. Another node acquires the lock and writes data.
 * When Node 1 wakes up, it does not know its lock expired, and proceeds to write outdated data, causing silent corruption!"</p>
 *
 * <h2>How Fencing Tokens Solve This:</h2>
 * <ol>
 *   <li>The Lock Service issues a <b>strictly monotonically increasing token</b> (e.g. 101, 102, 103...) every time a lock is acquired.</li>
 *   <li>The client passes this fencing token to the storage layer (Database / File System / S3).</li>
 *   <li>The storage layer rejects any write with a token that is smaller than or equal to the maximum token it has already processed:
 *       <pre>{@code
 *       UPDATE resource_table
 *       SET content = :content, last_fencing_token = :token
 *       WHERE id = :id AND :token > last_fencing_token
 *       }</pre>
 *   </li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FencingTokenPattern {

    private final StringRedisTemplate redisTemplate;
    private static final String FENCING_TOKEN_COUNTER_KEY = "lock:fencing:counter";

    @Getter
    public static class LockWithToken {
        private final String lockKey;
        private final long fencingToken;
        private final boolean acquired;

        public LockWithToken(String lockKey, long fencingToken, boolean acquired) {
            this.lockKey = lockKey;
            this.fencingToken = fencingToken;
            this.acquired = acquired;
        }
    }

    /**
     * Generates a monotonically increasing fencing token upon lock acquisition.
     */
    public LockWithToken acquireLockWithFencingToken(String lockKey) {
        // Atomic increment in Redis guarantees monotonic sequence
        Long token = redisTemplate.opsForValue().increment(FENCING_TOKEN_COUNTER_KEY);
        if (token == null) {
            throw new IllegalStateException("Failed to generate fencing token");
        }
        return new LockWithToken(lockKey, token, true);
    }

    /**
     * Storage layer validation simulation:
     * Rejects writes from clients whose fencing token is stale.
     */
    public static class StorageServiceWithFencing {
        private final AtomicLong lastProcessedToken = new AtomicLong(0);

        public synchronized boolean writeData(long clientFencingToken, String data) {
            if (clientFencingToken <= lastProcessedToken.get()) {
                log.error("FENCING VIOLATION: Stale write rejected! Client Token: {}, Storage Token: {}",
                        clientFencingToken, lastProcessedToken.get());
                return false; // Stale client write blocked!
            }

            lastProcessedToken.set(clientFencingToken);
            log.info("Write accepted with fencing token {}: data='{}'", clientFencingToken, data);
            return true;
        }

        public long getLastProcessedToken() {
            return lastProcessedToken.get();
        }
    }
}
