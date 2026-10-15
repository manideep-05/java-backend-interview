package normal;

/** Authenticated: withdrawals allowed. Verify balance AND atm cash first. */
public class AuthenticatedState implements ATMState {
    private final ATM atm;

    public AuthenticatedState(ATM atm) {
        this.atm = atm;
    }

    @Override public void insertCard(String cardNo) { System.out.println("Transaction in progress."); }
    @Override public void enterPin(String pin) { System.out.println("Already authenticated."); }

    @Override
    public void withdraw(long amount) {
        if (atm.getCashOnHand() < amount) {
            System.out.println("ATM has insufficient cash.");
            return;
        }
        Account account = atm.getBank().getAccount(atm.getCurrentCard());
        if (!account.debit(amount)) { // check-then-act inside Account (normal)
            System.out.println("Insufficient account balance.");
            return;
        }
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
