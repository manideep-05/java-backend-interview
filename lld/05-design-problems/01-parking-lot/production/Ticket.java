package production;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * PRODUCTION ticket.
 *
 * `closed` is an AtomicBoolean so that double-exit (the same ticket
 * presented twice, possibly concurrently) is idempotent: only the first
 * close() wins, preventing a double charge / double free.
 */
public class Ticket {
    private final String id;
    private final Vehicle vehicle;
    private final ParkingSpot spot;
    private final Instant entryTime;
    private volatile Instant exitTime;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public Ticket(String id, Vehicle vehicle, ParkingSpot spot, Instant entryTime) {
        this.id = id;
        this.vehicle = vehicle;
        this.spot = spot;
        this.entryTime = entryTime;
    }

    /** Returns true only for the FIRST caller that closes the ticket. */
    public boolean tryClose(Instant exitTime) {
        if (closed.compareAndSet(false, true)) {
            this.exitTime = exitTime;
            return true;
        }
        return false;
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
