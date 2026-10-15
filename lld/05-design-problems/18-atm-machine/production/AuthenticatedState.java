package production;

public class AuthenticatedState implements ATMState {
    private final ATM atm;

    public AuthenticatedState(ATM atm) {
        this.atm = atm;
    }

    @Override public void insertCard(String cardNo) { System.out.println("Transaction in progress."); }
    @Override public void enterPin(String pin) { System.out.println("Already authenticated."); }

    @Override
    public void withdraw(long amount) {
        // Reserve ATM cash atomically first (so we don't debit then find no cash).
        if (!atm.tryReserveCash(amount)) {
            System.out.println("ATM has insufficient cash.");
            return;
        }
        Account account = atm.getBank().getAccount(atm.getCurrentCard());
        // Atomic debit: no overdraft even under concurrent withdrawals.
        if (!account.debit(amount)) {
            atm.refundCash(amount); // give the reserved ATM cash back
            System.out.println("Insufficient account balance.");
            return;
        }
        // In reality debit + dispense is a transaction; if dispense failed
        // here we would credit() the account back to compensate.
        atm.dispense(amount);
        System.out.println("Dispensed " + amount + ". New balance: " + account.getBalance());
        ejectCard();
    }

    @Override
    public void ejectCard() {
        System.out.println("Card ejected.");
        atm.reset();
    }
}
