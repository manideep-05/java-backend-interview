package normal;

/** HasCard: authenticate via PIN; lock out after too many attempts. */
public class HasCardState implements ATMState {
    private static final int MAX_ATTEMPTS = 3;
    private final ATM atm;
    private int attempts = 0;

    public HasCardState(ATM atm) {
        this.atm = atm;
    }

    @Override public void insertCard(String cardNo) { System.out.println("Card already inserted."); }

    @Override
    public void enterPin(String pin) {
        if (atm.getBank().authenticate(atm.getCurrentCard(), pin)) {
            System.out.println("Authenticated.");
            attempts = 0;
            atm.setState(atm.getAuthenticatedState());
        } else {
            attempts++;
            System.out.println("Wrong PIN (" + attempts + "/" + MAX_ATTEMPTS + ")");
            if (attempts >= MAX_ATTEMPTS) {
                System.out.println("Too many attempts. Ejecting card.");
                ejectCard();
            }
        }
    }

    @Override public void withdraw(long amount) { System.out.println("Authenticate first."); }

    @Override
    public void ejectCard() {
        System.out.println("Card ejected.");
        attempts = 0;
        atm.reset();
    }
}
