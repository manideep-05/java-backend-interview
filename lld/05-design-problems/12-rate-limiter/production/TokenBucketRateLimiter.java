package production;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe token bucket.
 *
 * Concurrency design:
 *  - buckets: ConcurrentHashMap + computeIfAbsent so creating a client's
 *    bucket is atomic (no two threads create two buckets for one client).
 *  - Per-bucket synchronization: refill+consume is a check-then-act on
 *    the bucket's token count, so it MUST be atomic. We synchronize on
 *    the Bucket object itself. Crucially the lock is PER CLIENT — two
 *    different clients never block each other, so throughput scales.
 *  - System.nanoTime(): monotonic, immune to wall-clock jumps.
 *
 * (A fully lock-free version would CAS a packed (tokens,timestamp) long
 * in a loop; synchronized-per-bucket is simpler and contention is low
 * because it's scoped to a single client.)
 */
public class TokenBucketRateLimiter implements RateLimiter {

    private static final class Bucket {
        double tokens;
        long lastRefillNanos;

        Bucket(double tokens, long now) {
            this.tokens = tokens;
            this.lastRefillNanos = now;
        }
    }

    private final int capacity;
    private final double refillRatePerSec;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public TokenBucketRateLimiter(int capacity, double refillRatePerSec) {
        this.capacity = capacity;
        this.refillRatePerSec = refillRatePerSec;
    }

    @Override
    public boolean allow(String clientId) {
        long now = System.nanoTime();
        // Atomic get-or-create of this client's bucket.
        Bucket bucket = buckets.computeIfAbsent(clientId, id -> new Bucket(capacity, now));

        // Lock only THIS client's bucket: refill + consume as one atomic step.
        synchronized (bucket) {
            double elapsedSec = (now - bucket.lastRefillNanos) / 1_000_000_000.0;
            bucket.tokens = Math.min(capacity, bucket.tokens + elapsedSec * refillRatePerSec);
            bucket.lastRefillNanos = now;

            if (bucket.tokens >= 1.0) {
                bucket.tokens -= 1.0;
                return true;
            }
            return false;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        RateLimiter limiter = new TokenBucketRateLimiter(100, 50.0);
        java.util.concurrent.atomic.AtomicInteger allowed = new java.util.concurrent.atomic.AtomicInteger();

        // 10 threads x 100 requests for the SAME client: with capacity 100
        // and near-zero elapsed time, roughly 100 should be allowed, never
        // more than capacity+a tiny refill. No double-spend of a token.
        Runnable r = () -> {
            for (int i = 0; i < 100; i++) {
                if (limiter.allow("userA")) {
                    allowed.incrementAndGet();
                }
            }
        };
        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(r);
            threads[i].start();
        }
        for (Thread t : threads) {
            t.join();
        }
        System.out.println("Allowed ~" + allowed.get() + " (bounded by capacity, no token double-spend)");
    }
}
