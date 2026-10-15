package normal;

/**
 * CONTEXT for the State pattern.
 *
 * Holds the current state and the shared data (inventory, selection,
 * inserted amount), and simply delegates each operation to the current
 * state. No branching on state lives here — that's the whole point.
 */
public class VendingMachine {

    private final Inventory inventory = new Inventory();

    // Pre-created state instances (stateless w.r.t. the flow, so reusable).
    private final State idleState = new IdleState(this);
    private final State hasMoneyState = new HasMoneyState(this);
    private final State dispensingState = new DispensingState(this);

    private State state = idleState;
    private String selectedCode;
    private int insertedCents;

    // ---- delegation: context forwards to the current state ----
    public void selectProduct(String code) { state.selectProduct(code); }
    public void insertMoney(int cents) { state.insertMoney(cents); }
    public void dispense() { state.dispense(); }
    public void cancel() { state.cancel(); }

    // ---- state + data accessors used by the states ----
    public void setState(State state) { this.state = state; }
    public State getState() { return state; }
    public State getIdleState() { return idleState; }
    public State getHasMoneyState() { return hasMoneyState; }
    public State getDispensingState() { return dispensingState; }

    public Inventory getInventory() { return inventory; }
    public void setSelectedCode(String code) { this.selectedCode = code; }
    public String getSelectedCode() { return selectedCode; }
    public void addInserted(int cents) { this.insertedCents += cents; }
    public int getInserted() { return insertedCents; }

    /** Return to Idle and clear the transaction. */
    public void reset() {
        this.insertedCents = 0;
        this.selectedCode = null;
        this.state = idleState;
    }

    public static void main(String[] args) {
        VendingMachine machine = new VendingMachine();
        machine.getInventory().add(new Product("A1", 150), 2); // $1.50, 2 units

        machine.selectProduct("A1");  // -> HasMoney
        machine.insertMoney(100);     // 100
        machine.insertMoney(100);     // 200
        machine.dispense();           // dispenses, change 50
        // Back to Idle; a second flow would work the same way.
    }
}
