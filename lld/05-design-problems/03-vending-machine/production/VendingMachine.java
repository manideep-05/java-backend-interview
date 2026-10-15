package production;

/**
 * PRODUCTION context.
 *
 * Concurrency design:
 *  - Every public operation (select/insert/dispense/cancel) is
 *    `synchronized` on the machine. A physical machine processes one
 *    user interaction at a time; serializing operations guarantees the
 *    (state, selectedCode, insertedCents) tuple always transitions
 *    consistently, even if a coin sensor and a button fire concurrently.
 *  - Lock scope is the whole machine because a transition reads AND
 *    writes several fields together (classic compound action).
 *  - Inventory decrement is additionally atomic (CAS) so even if the
 *    design were later made more concurrent, we never oversell.
 */
public class VendingMachine {

    private final Inventory inventory = new Inventory();

    private final State idleState = new IdleState(this);
    private final State hasMoneyState = new HasMoneyState(this);
    private final State dispensingState = new DispensingState(this);

    private State state = idleState;
    private String selectedCode;
    private int insertedCents;

    // Each operation is atomic w.r.t. other operations on this machine.
    public synchronized void selectProduct(String code) { state.selectProduct(code); }
    public synchronized void insertMoney(int cents) { state.insertMoney(cents); }
    public synchronized void dispense() { state.dispense(); }
    public synchronized void cancel() { state.cancel(); }

    // Accessors used by states; called only while holding the machine lock
    // (states run inside the synchronized operations above).
    public void setState(State state) { this.state = state; }
    public State getState() { return state; }
    public State getHasMoneyState() { return hasMoneyState; }
    public State getDispensingState() { return dispensingState; }

    public Inventory getInventory() { return inventory; }
    public void setSelectedCode(String code) { this.selectedCode = code; }
    public String getSelectedCode() { return selectedCode; }
    public void addInserted(int cents) { this.insertedCents += cents; }
    public int getInserted() { return insertedCents; }

    public void reset() {
        this.insertedCents = 0;
        this.selectedCode = null;
        this.state = idleState;
    }

    public static void main(String[] args) {
        VendingMachine machine = new VendingMachine();
        machine.getInventory().add(new Product("A1", 150), 1);

        machine.selectProduct("A1");
        machine.insertMoney(100);
        machine.insertMoney(100);
        machine.dispense(); // dispenses, change 50; sold out afterward
    }
}
