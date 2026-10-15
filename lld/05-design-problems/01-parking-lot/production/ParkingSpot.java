package production;

import java.util.concurrent.atomic.AtomicReference;

/**
 * PRODUCTION spot.
 *
 * The occupancy is an AtomicReference<Vehicle>. tryAssign() uses a
 * compare-and-set (CAS): it atomically moves the slot from "free" (null)
 * to "holds this vehicle" ONLY if it was free. This closes the
 * check-then-act race — two threads calling tryAssign() on the same spot,
 * exactly one gets `true`. No lock needed at the spot level.
 */
public class ParkingSpot {
    private final String id;
    private final SpotType type;
    private final AtomicReference<Vehicle> occupant = new AtomicReference<>(null);
    private Level level; // set by the owning Level when the spot is added

    public ParkingSpot(String id, SpotType type) {
        this.id = id;
        this.type = type;
    }

    void setLevel(Level level) {
        this.level = level;
    }

    public Level getLevel() {
        return level;
    }

    public boolean isFree() {
        return occupant.get() == null;
    }

    public boolean canFit(VehicleType vehicleType) {
        return type.canFit(vehicleType);
    }

    /**
     * Atomically claim this spot. Returns true if THIS call won the spot.
     * CAS(expected=null, new=vehicle) succeeds for exactly one racing thread.
     */
    public boolean tryAssign(Vehicle v) {
        if (!type.canFit(v.getType())) {
            return false;
        }
        return occupant.compareAndSet(null, v);
    }

    public void free() {
        occupant.set(null);
    }

    public String getId() {
        return id;
    }

    public SpotType getType() {
        return type;
    }
}
