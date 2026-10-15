package production;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * PRODUCTION agent.
 *
 * `busy` is an AtomicBoolean; claim() CAS-flips false -> true, so two
 * concurrent orders can never both claim the same agent. No lock needed.
 */
public class DeliveryAgent {
    private final String id;
    private final int location;
    private final AtomicBoolean busy = new AtomicBoolean(false);

    public DeliveryAgent(String id, int location) {
        this.id = id;
        this.location = location;
    }

    public boolean claim() {
        return busy.compareAndSet(false, true);
    }

    public void free() {
        busy.set(false);
    }

    public boolean isBusy() { return busy.get(); }
    public int getLocation() { return location; }
    public String getId() { return id; }
}
