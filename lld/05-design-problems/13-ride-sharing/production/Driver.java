package production;

import java.util.concurrent.atomic.AtomicReference;

/**
 * PRODUCTION driver.
 *
 * status is an AtomicReference<DriverStatus>. claim() CAS-flips
 * AVAILABLE -> BUSY; exactly one of two racing riders wins, so a driver
 * can never be assigned to two rides at once. No lock needed.
 */
public class Driver {
    private final String id;
    private final int location;
    private final AtomicReference<DriverStatus> status =
            new AtomicReference<>(DriverStatus.AVAILABLE);

    public Driver(String id, int location) {
        this.id = id;
        this.location = location;
    }

    public boolean claim() {
        return status.compareAndSet(DriverStatus.AVAILABLE, DriverStatus.BUSY);
    }

    public void release() {
        status.set(DriverStatus.AVAILABLE);
    }

    public DriverStatus getStatus() { return status.get(); }
    public int getLocation() { return location; }
    public String getId() { return id; }
}
