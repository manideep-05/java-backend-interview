package normal;

/**
 * A single parking spot. Owns its own occupancy state and the rule for
 * whether a vehicle can park here (delegating the size rule to SpotType).
 *
 * NOTE (normal version): isFree()/assign() are two separate steps. In a
 * single-threaded walkthrough that's fine; the production version makes
 * the claim atomic to avoid the "two cars, one spot" race.
 */
public class ParkingSpot {
    private final String id;
    private final SpotType type;
    private Vehicle vehicle; // null => free

    public ParkingSpot(String id, SpotType type) {
        this.id = id;
        this.type = type;
    }

    public boolean isFree() {
        return vehicle == null;
    }

    public boolean canFit(Vehicle v) {
        return isFree() && type.canFit(v.getType());
    }

    public void assign(Vehicle v) {
        this.vehicle = v;
    }

    public void free() {
        this.vehicle = null;
    }

    public String getId() {
        return id;
    }

    public SpotType getType() {
        return type;
    }
}
