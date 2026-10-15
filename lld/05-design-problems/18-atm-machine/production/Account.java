package production;

/**
 * PRODUCTION account.
 *
 * debit() is `synchronized` so the verify-then-subtract is ONE atomic
 * critical section. This is the fix for the overdraft race: two
 * withdrawals on the same account (from two ATMs/threads) can no longer
 * both pass the balance check on the same value. Exactly one succeeds if
 * only one amount fits.
 *
 * (In a real distributed bank this becomes a DB transaction with a row
 * lock or an optimistic version column; the principle is identical:
 * make verify+debit atomic.)
 */
public class Account {
    private final String id;
    private long balanceCents;

    public Account(String id, long balanceCents) {
        this.id = id;
        this.balanceCents = balanceCents;
    }

    public synchronized boolean debit(long amount) {
        if (balanceCents >= amount) {
            balanceCents -= amount;
            return true;
        }
        return false;
    }

    public synchronized void credit(long amount) {
        balanceCents += amount; // used for compensation/reversal
    }

    public synchronized long getBalance() {
        return balanceCents;
    }

    public String getId() {
        return id;
    }
}
