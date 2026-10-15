package production;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PRODUCTION level.
 *
 * Concurrency design choices:
 *  - spots are in a CopyOnWriteArrayList: the set of spots rarely changes
 *    after setup but is iterated by every park(), so copy-on-write gives
 *    lock-free reads.
 *  - availableCounts: per-SpotType AtomicInteger so availability is O(1)
 *    and updated atomically on claim/free, instead of scanning spots
 *    under a lock.
 *  - The actual claim relies on ParkingSpot.tryAssign (CAS), so we do NOT
 *    need a coarse lock around the scan: we scan lock-free and let CAS
 *    arbitrate the winner for the last spot.
 */
public class Level {
    private final int floor;
    private final List<ParkingSpot> spots = new CopyOnWriteArrayList<>();
    private final Map<SpotType, AtomicInteger> availableCounts = new ConcurrentHashMap<>();

    public Level(int floor) {
        this.floor = floor;
        for (SpotType t : SpotType.values()) {
            availableCounts.put(t, new AtomicInteger(0));
        }
    }

    public void addSpot(ParkingSpot spot) {
        spot.setLevel(this); // back-reference so a spot can be released via its level
        spots.add(spot);
        availableCounts.get(spot.getType()).incrementAndGet();
    }

    /**
     * Try to park on this level. Scans lock-free; the first spot whose
     * atomic CAS claim succeeds is ours. If another thread grabbed a
     * candidate between our isFree() check and tryAssign(), tryAssign()
     * simply returns false and we continue scanning — no spot is ever
     * double-assigned.
     */
    public ParkingSpot tryPark(Vehicle vehicle) {
        for (ParkingSpot spot : spots) {
            if (spot.canFit(vehicle.getType()) && spot.tryAssign(vehicle)) {
                availableCounts.get(spot.getType()).decrementAndGet();
                return spot;
            }
        }
        return null;
    }

    public void release(ParkingSpot spot) {
        spot.free();
        availableCounts.get(spot.getType()).incrementAndGet();
    }

    public int availableSpots(SpotType type) {
        return availableCounts.get(type).get();
    }

    public int getFloor() {
        return floor;
    }
}
