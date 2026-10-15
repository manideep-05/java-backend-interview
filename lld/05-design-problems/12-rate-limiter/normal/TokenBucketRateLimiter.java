package normal;

import java.util.HashMap;
import java.util.Map;

/**
 * Token bucket — single-threaded.
 *
 * Each client has a Bucket with up to `capacity` tokens that refill at
 * `refillRatePerSec`. A request spends one token. Refill is LAZY: we
 * compute how many tokens to add based on elapsed time at call time, so
 * no background thread is needed.
 */
public class TokenBucketRateLimiter implements RateLimiter {

    private static class Bucket {
        double tokens;
        long lastRefillNanos;

        Bucket(double tokens, long now) {
            this.tokens = tokens;
            this.lastRefillNanos = now;
        }
    }

    private final int capacity;
    private final double refillRatePerSec;
    private final Map<String, Bucket> buckets = new HashMap<>();

    public TokenBucketRateLimiter(int capacity, double refillRatePerSec) {
        this.capacity = capacity;
        this.refillRatePerSec = refillRatePerSec;
    }

    @Override
    public boolean allow(String clientId) {
        long now = System.nanoTime(); // monotonic clock for elapsed time
        Bucket bucket = buckets.computeIfAbsent(clientId, id -> new Bucket(capacity, now));

        // Lazy refill: add tokens proportional to elapsed time, capped.
        double elapsedSec = (now - bucket.lastRefillNanos) / 1_000_000_000.0;
        bucket.tokens = Math.min(capacity, bucket.tokens + elapsedSec * refillRatePerSec);
        bucket.lastRefillNanos = now;

        // Check-then-act: safe here because single-threaded.
        if (bucket.tokens >= 1.0) {
            bucket.tokens -= 1.0;
            return true;
        }
        return false;
    }

    public static void main(String[] args) {
        // capacity 3, refill 1 token/sec
        RateLimiter limiter = new TokenBucketRateLimiter(3, 1.0);
        for (int i = 1; i <= 5; i++) {
            System.out.println("req " + i + " -> " + limiter.allow("userA"));
        }
        // First 3 allowed (burst up to capacity), rest denied until refill.
    }
}
