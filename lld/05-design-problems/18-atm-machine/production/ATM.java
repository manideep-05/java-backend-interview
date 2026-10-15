package production;

import java.util.concurrent.atomic.AtomicLong;

/**
 * PRODUCTION context.
 *
 * Concurrency design:
 *  - cashOnHand is an AtomicLong; tryReserveCash() uses a CAS loop so two
 *    withdrawals can't both claim the same last notes.
 *  - The withdraw critical steps are: reserve ATM cash (atomic) THEN
 *    atomically debit the account. Ordering reserve-before-debit lets us
 *    cleanly refund the ATM cash if the debit fails.
 *  - Account.debit() is itself atomic (synchronized), preventing overdraft.
 *  - Per-session fields (currentCard, state) are confined to one user
 *    session; a real ATM processes one card at a time, so we don't lock
 *    those — the shared contention point is the ACCOUNT, which is guarded.
 */
public class ATM {
    private final BankService bank;
    private final AtomicLong cashOnHand;

    private final ATMState idleState = new IdleState(this);
    private final ATMState hasCardState = new HasCardState(this);
    private final ATMState authenticatedState = new AuthenticatedState(this);

    private volatile ATMState state = idleState;
    private volatile String currentCard;

    public ATM(BankService bank, long cashOnHand) {
        this.bank = bank;
        this.cashOnHand = new AtomicLong(cashOnHand);
    }

    public void insertCard(String cardNo) { state.insertCard(cardNo); }
    public void enterPin(String pin) { state.enterPin(pin); }
    public void withdraw(long amount) { state.withdraw(amount); }
    public void ejectCard() { state.ejectCard(); }

    /** Atomically reserve ATM cash; false if not enough. */
    public boolean tryReserveCash(long amount) {
        while (true) {
            long cur = cashOnHand.get();
            if (cur < amount) {
                return false;
            }
            if (cashOnHand.compareAndSet(cur, cur - amount)) {
                return true;
            }
        }
    }

    public void refundCash(long amount) { cashOnHand.addAndGet(amount); }
    public void dispense(long amount) { /* cash already reserved; physically dispense */ }

    public void setState(ATMState state) { this.state = state; }
    public ATMState getHasCardState() { return hasCardState; }
    public ATMState getAuthenticatedState() { return authenticatedState; }

    public BankService getBank() { return bank; }
    public void setCurrentCard(String card) { this.currentCard = card; }
    public String getCurrentCard() { return currentCard; }
    public long getCashOnHand() { return cashOnHand.get(); }

    public void reset() {
        this.currentCard = null;
        this.state = idleState;
    }

    public static void main(String[] args) throws InterruptedException {
        BankService bank = new BankService();
        Account shared = new Account("ACC-1", 30000); // $300 — enough for ONE $200 withdrawal
        bank.register("CARD-1", "1234", shared);

        // Two ATMs hitting the SAME account concurrently for $200 each.
        ATM atm1 = new ATM(bank, 100000);
        ATM atm2 = new ATM(bank, 100000);

        Runnable w1 = () -> { atm1.insertCard("CARD-1"); atm1.enterPin("1234"); atm1.withdraw(20000); };
        Runnable w2 = () -> { atm2.insertCard("CARD-1"); atm2.enterPin("1234"); atm2.withdraw(20000); };
        Thread t1 = new Thread(w1), t2 = new Thread(w2);
        t1.start(); t2.start(); t1.join(); t2.join();

        // Exactly ONE $200 withdrawal succeeds; balance never goes negative.
        System.out.println("Final balance: " + shared.getBalance());
    }
}
