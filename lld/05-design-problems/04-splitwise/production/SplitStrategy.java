package production;

import java.util.List;
import java.util.Map;

/** STRATEGY: compute exact per-participant shares in cents (sums to total). */
public interface SplitStrategy {
    Map<String, Long> computeShares(long totalCents, List<String> participants, long[] args);
}
