package normal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Exact split: caller supplies each participant's exact share in `args`.
 * We VALIDATE that the shares sum to the total — otherwise reject,
 * because an expense that doesn't reconcile is a data bug.
 */
public class ExactSplitStrategy implements SplitStrategy {
    @Override
    public Map<String, Long> computeShares(long totalCents, List<String> participants, long[] args) {
        if (args.length != participants.size()) {
            throw new IllegalArgumentException("Need one exact share per participant");
        }
        long sum = 0;
        Map<String, Long> shares = new LinkedHashMap<>();
        for (int i = 0; i < participants.size(); i++) {
            shares.put(participants.get(i), args[i]);
            sum += args[i];
        }
        if (sum != totalCents) {
            throw new IllegalArgumentException("Exact shares (" + sum + ") must sum to total (" + totalCents + ")");
        }
        return shares;
    }
}
