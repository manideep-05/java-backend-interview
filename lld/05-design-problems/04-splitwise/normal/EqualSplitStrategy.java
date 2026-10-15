package normal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Equal split with DETERMINISTIC remainder-cent distribution.
 *
 * $10 among 3 => 334, 333, 333 (first `remainder` people pay +1 cent).
 * This guarantees the shares sum EXACTLY to the total — no lost cents,
 * no floating point.
 */
public class EqualSplitStrategy implements SplitStrategy {
    @Override
    public Map<String, Long> computeShares(long totalCents, List<String> participants, long[] args) {
        int n = participants.size();
        long base = totalCents / n;
        long remainder = totalCents % n; // this many people pay one extra cent

        Map<String, Long> shares = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            long share = base + (i < remainder ? 1 : 0);
            shares.put(participants.get(i), share);
        }
        return shares;
    }
}
