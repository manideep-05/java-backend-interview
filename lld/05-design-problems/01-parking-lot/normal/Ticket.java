package normal;

import java.time.Instant;

/**
 * A parking ticket: the record issued at entry and settled at exit.
 * Immutable except for the exit time / status set on unpark. It holds a
 * reference to the spot so unpark knows what to free.
 */
public class Ticket {
    private final String id;
    private final Vehicle vehicle;
    private final ParkingSpot spot;
    private final Instant entryTime;
    private Instant exitTime;
    private boolean closed; // guards against double exit (idempotency)

    public Ticket(String id, Vehicle vehicle, ParkingSpot spot, Instant entryTime) {
        this.id = id;
        this.vehicle = vehicle;
        this.spot = spot;
        this.entryTime = entryTime;
    }

    public void close(Instant exitTime) {
        this.exitTime = exitTime;
        this.closed = true;
    }

    public boolean isClosed() {
        return closed;
    }

    public String getId() {
        return id;
    }

    public ParkingSpot getSpot() {
        return spot;
    }

    public Instant getEntryTime() {
        return entryTime;
    }

    public Instant getExitTime() {
        return exitTime;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }
}
