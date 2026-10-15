package normal;

import java.util.List;

/**
 * STRATEGY pattern — the spot-allocation policy.
 *
 * Decouples "which free spot do we pick?" from the lot itself. Swapping
 * first-fit for nearest-to-entrance means adding a new implementation,
 * not editing the lot (OCP + DIP).
 */
public interface ParkingStrategy {
    ParkingSpot findSpot(List<ParkingSpot> spots, Vehicle vehicle);
}
