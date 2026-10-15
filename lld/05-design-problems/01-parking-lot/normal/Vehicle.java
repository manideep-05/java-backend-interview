package normal;

/**
 * Vehicle entity. Kept as a simple value object: a license plate + its
 * type. Behavior (pricing, allocation) lives elsewhere so this stays a
 * single-responsibility data holder.
 */
public class Vehicle {
    private final String licensePlate;
    private final VehicleType type;

    public Vehicle(String licensePlate, VehicleType type) {
        this.licensePlate = licensePlate;
        this.type = type;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public VehicleType getType() {
        return type;
    }
}
