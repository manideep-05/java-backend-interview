package production;

import java.util.List;

/**
 * Nearest-driver matching. Stateless => thread-safe to share. Note this
 * only PROPOSES a candidate; the actual claim (CAS) in RideService is
 * what guarantees exclusivity, so a stale "available" read here is fine.
 */
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
        return best;
    }
}
