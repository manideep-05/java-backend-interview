package normal;

import java.util.List;

/** Pick the closest AVAILABLE driver (naive linear scan over the pool). */
public class NearestDriverStrategy implements MatchingStrategy {
    @Override
    public Driver match(List<Driver> drivers, int pickupLocation) {
        Driver best = null;
        int bestDist = Integer.MAX_VALUE;
        for (Driver d : drivers) {
            if (d.getStatus() == DriverStatus.AVAILABLE) {
                int dist = Math.abs(d.getLocation() - pickupLocation);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = d;
                }
            }
        }
        return best; // null if none available
    }
}
