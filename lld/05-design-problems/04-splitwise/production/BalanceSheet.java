package production;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * PRODUCTION ledger.
 *
 * Concurrency design:
 *  - A ReentrantReadWriteLock guards the balance map. addDebt() does a
 *    read-modify-write across potentially TWO entries (netting opposite
 *    balances), so it must hold the WRITE lock for the whole compound
 *    operation — otherwise two concurrent expenses could interleave and
 *    lose an update.
 *  - get()/print() take the READ lock, so many readers run concurrently
 *    while writes are exclusive. This fits the typical read-heavy access
 *    pattern (balances viewed far more often than changed).
 */
public class BalanceSheet {
    private final Map<String, Map<String, Long>> balances = new HashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public void addDebt(String debtor, String creditor, long amount) {
        if (debtor.equals(creditor) || amount == 0) {
            return;
        }
        lock.writeLock().lock(); // compound read-modify-write => exclusive
        try {
            long existingOpposite = getUnlocked(creditor, debtor);
            if (existingOpposite > 0) {
                if (existingOpposite >= amount) {
                    setUnlocked(creditor, debtor, existingOpposite - amount);
                    return;
                } else {
                    setUnlocked(creditor, debtor, 0);
                    amount -= existingOpposite;
                }
            }
            setUnlocked(debtor, creditor, getUnlocked(debtor, creditor) + amount);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public long get(String a, String b) {
        lock.readLock().lock();
        try {
            return getUnlocked(a, b);
        } finally {
            lock.readLock().unlock();
        }
    }

    private long getUnlocked(String a, String b) {
        return balances.getOrDefault(a, Map.of()).getOrDefault(b, 0L);
    }

    private void setUnlocked(String a, String b, long value) {
        balances.computeIfAbsent(a, k -> new HashMap<>()).put(b, value);
    }

    public void print() {
        lock.readLock().lock();
        try {
            balances.forEach((a, m) -> m.forEach((b, amt) -> {
                if (amt > 0) {
                    System.out.println(a + " owes " + b + ": " + amt + " cents");
                }
            }));
        } finally {
            lock.readLock().unlock();
        }
    }
}
