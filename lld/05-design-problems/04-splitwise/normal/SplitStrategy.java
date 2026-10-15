package normal;

import java.util.List;
import java.util.Map;

/**
 * STRATEGY: compute each participant's share (in cents) of a total.
 * Implementations: equal, exact, percentage. The service doesn't care
 * which — add a new split type without touching it (OCP).
 *
 * Returns a map participant -> owedCents. Must sum EXACTLY to totalCents.
 */
public interface SplitStrategy {
    Map<String, Long> computeShares(long totalCents, List<String> participants, long[] args);
}
