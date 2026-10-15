package normal;

/** A delivery agent; FREE or BUSY. NOTE (normal): claim is check-then-set. */
public class DeliveryAgent {
    private final String id;
    private final int location;
    private boolean busy = false;

    public DeliveryAgent(String id, int location) {
        this.id = id;
        this.location = location;
    }

    public boolean claim() {
        if (!busy) {
            busy = true;
            return true;
        }
        return false;
    }

    public void free() {
        busy = false;
    }

    public boolean isBusy() { return busy; }
    public int getLocation() { return location; }
    public String getId() { return id; }
}
