package normal;

import java.util.HashMap;
import java.util.Map;

/**
 * The ledger: running net balances. balances.get(a).get(b) = how much
 * `a` owes `b`. We keep running nets (not a transaction log) so reads
 * are O(1). Single responsibility: balance arithmetic.
 */
public class BalanceSheet {
    private final Map<String, Map<String, Long>> balances = new HashMap<>();

    /** `debtor` owes `creditor` `amount` more cents. */
    public void addDebt(String debtor, String creditor, long amount) {
        if (debtor.equals(creditor) || amount == 0) {
            return; // no self-debt
        }
        // Net against any opposite balance to keep the ledger simple.
        long existingOpposite = get(creditor, debtor);
        if (existingOpposite > 0) {
            if (existingOpposite >= amount) {
                set(creditor, debtor, existingOpposite - amount);
                return;
            } else {
                set(creditor, debtor, 0);
                amount -= existingOpposite;
            }
        }
        set(debtor, creditor, get(debtor, creditor) + amount);
    }

    public long get(String a, String b) {
        return balances.getOrDefault(a, Map.of()).getOrDefault(b, 0L);
    }

    private void set(String a, String b, long value) {
        balances.computeIfAbsent(a, k -> new HashMap<>()).put(b, value);
    }

    public void print() {
        balances.forEach((a, m) -> m.forEach((b, amt) -> {
            if (amt > 0) {
                System.out.println(a + " owes " + b + ": " + amt + " cents");
            }
        }));
    }
}
