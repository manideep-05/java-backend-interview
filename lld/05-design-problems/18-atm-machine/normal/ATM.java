package normal;

/**
 * CONTEXT. Holds current state, the card in use, cash on hand, and the
 * BankService. Delegates every operation to the current state (State
 * pattern); no state branching lives here.
 */
public class ATM {
    private final BankService bank;

    private final ATMState idleState = new IdleState(this);
    private final ATMState hasCardState = new HasCardState(this);
    private final ATMState authenticatedState = new AuthenticatedState(this);

    private ATMState state = idleState;
    private String currentCard;
    private long cashOnHand;

    public ATM(BankService bank, long cashOnHand) {
        this.bank = bank;
        this.cashOnHand = cashOnHand;
    }

    public void insertCard(String cardNo) { state.insertCard(cardNo); }
    public void enterPin(String pin) { state.enterPin(pin); }
    public void withdraw(long amount) { state.withdraw(amount); }
    public void ejectCard() { state.ejectCard(); }

    public void dispense(long amount) { cashOnHand -= amount; }

    public void setState(ATMState state) { this.state = state; }
    public ATMState getHasCardState() { return hasCardState; }
    public ATMState getAuthenticatedState() { return authenticatedState; }

    public BankService getBank() { return bank; }
    public void setCurrentCard(String card) { this.currentCard = card; }
    public String getCurrentCard() { return currentCard; }
    public long getCashOnHand() { return cashOnHand; }

    public void reset() {
        this.currentCard = null;
        this.state = idleState;
    }

    public static void main(String[] args) {
        BankService bank = new BankService();
        bank.register("CARD-1", "1234", new Account("ACC-1", 50000)); // $500.00
        ATM atm = new ATM(bank, 100000);

        atm.insertCard("CARD-1");
        atm.enterPin("1234");
        atm.withdraw(20000); // $200 -> balance 30000, card ejected
    }
}
