package normal;

import java.util.ArrayList;
import java.util.List;

/**
 * FACADE. Owns the driver pool and the matching strategy; turns a ride
 * request into an assigned driver.
 */
public class RideService {
    private final List<Driver> drivers = new ArrayList<>();
    private final MatchingStrategy matchingStrategy;

    public RideService(MatchingStrategy matchingStrategy) {
        this.matchingStrategy = matchingStrategy;
    }

    public void addDriver(Driver d) {
        drivers.add(d);
    }

    public Driver requestRide(String rider, int pickupLocation) {
        Driver driver = matchingStrategy.match(drivers, pickupLocation);
        if (driver == null || !driver.claim()) { // check-then-claim (normal)
            System.out.println("No driver available for " + rider);
            return null;
        }
        System.out.println(rider + " matched with driver " + driver.getId());
        return driver;
    }

    public static void main(String[] args) {
        RideService service = new RideService(new NearestDriverStrategy());
        service.addDriver(new Driver("D1", 2));
        service.addDriver(new Driver("D2", 10));

        service.requestRide("alice", 3); // -> D1 (nearest)
        service.requestRide("bob", 9);   // -> D2
    }
}
