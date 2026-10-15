package normal;

/**
 * Account: the money source of truth. Balance in cents.
 *
 * NOTE (normal): debit() is a plain check-then-subtract. Fine single-
 * threaded; the production version makes it atomic to stop overdraft
 * via concurrent withdrawals.
 */
public class Account {
    private final String id;
    private long balanceCents;

    public Account(String id, long balanceCents) {
        this.id = id;
        this.balanceCents = balanceCents;
    }

    public boolean debit(long amount) {
        if (balanceCents >= amount) {
            balanceCents -= amount; // check-then-act gap (not atomic here)
            return true;
        }
        return false;
    }

    public void credit(long amount) {
        balanceCents += amount;
    }

    public long getBalance() {
        return balanceCents;
    }

    public String getId() {
        return id;
    }
}
