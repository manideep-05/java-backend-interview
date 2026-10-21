package com.concurrency.locking.production_patterns;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * <h1>EXPONENTIAL BACKOFF WITH FULL JITTER RETRY TEMPLATE</h1>
 *
 * <h2>Why Standard Fixed or Linear Retry Causes Cascading Failures:</h2>
 * <p>If 100 threads fail to acquire a lock at 12:00:00.000 and all retry after exactly 100ms,
 * all 100 threads hit the database or Redis again simultaneously at 12:00:00.100.
 * This periodic collision wave is known as the <b>Thundering Herd / Retry Wave problem</b>.</p>
 *
 * <h2>The Solution: Full Jitter Algorithm (AWS Architecture Recommendation):</h2>
 * <pre>{@code
 * sleep = rand(0, min(maxBackoff, baseBackoff * (2 ^ attempt)))
 * }</pre>
 * <p>Jitter spreads out retry attempts evenly across time, minimizing collision probability and maximizing throughput.</p>
 */
@Component
@Slf4j
public class LockRetryWithJitterTemplate {

    /**
     * Executes a task with exponential backoff and full jitter upon failure.
     */
    public <T> T executeWithRetry(
            Supplier<T> task,
            int maxAttempts,
            long baseBackoffMs,
            long maxBackoffMs,
            Class<? extends Throwable> retryableException
    ) throws Exception {
        int attempt = 0;

        while (true) {
            try {
                attempt++;
                return task.get();
            } catch (Throwable ex) {
                if (!retryableException.isAssignableFrom(ex.getClass()) || attempt >= maxAttempts) {
                    log.error("Execution failed permanently after {} attempts or non-retryable exception: {}",
                            attempt, ex.getMessage());
                    throw ex;
                }

                // Calculate exponential backoff ceiling: base * 2^attempt
                long backoffCeiling = Math.min(maxBackoffMs, baseBackoffMs * (1L << (attempt - 1)));

                // Full Jitter: choose uniformly between 0 and backoffCeiling
                long jitteredSleepMs = ThreadLocalRandom.current().nextLong(0, backoffCeiling + 1);

                log.warn("Attempt {} failed with [{}]. Backing off for {} ms before retry...",
                        attempt, ex.getClass().getSimpleName(), jitteredSleepMs);

                try {
                    Thread.sleep(jitteredSleepMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Thread interrupted during retry backoff", ie);
                }
            }
        }
    }
}
