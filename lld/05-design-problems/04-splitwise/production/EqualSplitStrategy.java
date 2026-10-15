package production;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Equal split with deterministic remainder-cent distribution (stateless => thread-safe). */
public class EqualSplitStrategy implements SplitStrategy {
    @Override
    public Map<String, Long> computeShares(long totalCents, List<String> participants, long[] args) {
        int n = participants.size();
        long base = totalCents / n;
        long remainder = totalCents % n;
        Map<String, Long> shares = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            shares.put(participants.get(i), base + (i < remainder ? 1 : 0));
        }
        return shares;
    }
}
