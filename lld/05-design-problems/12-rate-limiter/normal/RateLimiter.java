package normal;

/**
 * STRATEGY seam: the rate-limiting algorithm behind one method.
 * Swapping token bucket for sliding window = a new implementation,
 * no change to callers (OCP, DIP).
 */
public interface RateLimiter {
    boolean allow(String clientId);
}
