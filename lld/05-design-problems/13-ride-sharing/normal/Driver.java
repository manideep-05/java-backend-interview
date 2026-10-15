package normal;

/**
 * Driver with a location (1-D for simplicity) and a status.
 * NOTE (normal): status is a plain field; claim is check-then-set.
 */
public class Driver {
    private final String id;
    private int location;
    private DriverStatus status = DriverStatus.AVAILABLE;

    public Driver(String id, int location) {
        this.id = id;
        this.location = location;
    }

    public boolean claim() {
        if (status == DriverStatus.AVAILABLE) {
            status = DriverStatus.BUSY;
            return true;
        }
        return false;
    }

    public void release() {
        status = DriverStatus.AVAILABLE;
    }

    public DriverStatus getStatus() { return status; }
    public int getLocation() { return location; }
    public String getId() { return id; }
}
