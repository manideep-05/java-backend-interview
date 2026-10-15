package normal;

import java.util.List;

/**
 * A concrete allocation strategy: return the first spot that fits.
 * Simple and O(n); good enough for the base design.
 */
public class FirstFitStrategy implements ParkingStrategy {
    @Override
    public ParkingSpot findSpot(List<ParkingSpot> spots, Vehicle vehicle) {
        for (ParkingSpot spot : spots) {
            if (spot.canFit(vehicle)) {
                return spot;
            }
        }
        return null; // caller treats null as "no spot available"
    }
}
