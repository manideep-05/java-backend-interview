package production;

/** STRATEGY seam for the algorithm (same contract as normal version). */
public interface RateLimiter {
    boolean allow(String clientId);
}
