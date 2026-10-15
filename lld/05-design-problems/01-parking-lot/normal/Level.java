package normal;

import java.util.ArrayList;
import java.util.List;

/**
 * A floor of the lot. Holds its spots and delegates "which spot?" to the
 * injected ParkingStrategy. One reason to change: how a level manages its
 * spots (SRP).
 */
public class Level {
    private final int floor;
    private final List<ParkingSpot> spots = new ArrayList<>();
    private final ParkingStrategy strategy;

    public Level(int floor, ParkingStrategy strategy) {
        this.floor = floor;
        this.strategy = strategy;
    }

    public void addSpot(ParkingSpot spot) {
        spots.add(spot);
    }

    /** Find a fitting spot and claim it. (normal: not atomic.) */
    public ParkingSpot parkVehicle(Vehicle vehicle) {
        ParkingSpot spot = strategy.findSpot(spots, vehicle);
        if (spot == null) {
            return null;
        }
        spot.assign(vehicle); // check-then-act gap: fine single-threaded
        return spot;
    }

    public long availableSpots(VehicleType vehicleType) {
        return spots.stream().filter(s -> s.isFree() && s.getType().canFit(vehicleType)).count();
    }

    public int getFloor() {
        return floor;
    }
}
