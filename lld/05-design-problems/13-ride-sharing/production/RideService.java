package production;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PRODUCTION facade.
 *
 * Concurrency design — PROPOSE then ATOMIC CLAIM, with retry:
 *  - The matcher proposes a candidate (possibly on stale availability).
 *  - We attempt an atomic claim() (CAS) on that candidate. If it fails
 *    (another rider grabbed it first), we RE-MATCH and try again,
 *    excluding busy drivers, until we claim one or the pool is exhausted.
 *  - This "optimistic match + CAS claim + retry" avoids any global lock:
 *    distinct drivers never contend, and the same driver is arbitrated
 *    by CAS. One driver is never assigned to two rides.
 *  - drivers is a CopyOnWriteArrayList (rarely changes, read every match).
 */
public class RideService {
    private final List<Driver> drivers = new CopyOnWriteArrayList<>();
    private final MatchingStrategy matchingStrategy;

    public RideService(MatchingStrategy matchingStrategy) {
        this.matchingStrategy = matchingStrategy;
    }

    public void addDriver(Driver d) {
        drivers.add(d);
    }

    public Driver requestRide(String rider, int pickupLocation) {
        while (true) {
            Driver candidate = matchingStrategy.match(drivers, pickupLocation);
            if (candidate == null) {
                return null; // no drivers available
            }
            if (candidate.claim()) { // atomic AVAILABLE -> BUSY
                return candidate;
            }
            // Lost the race for this candidate; loop to match a different one.
        }
    }

    public static void main(String[] args) throws InterruptedException {
        RideService service = new RideService(new NearestDriverStrategy());
        // Exactly ONE driver near the pickup; many riders race for it.
        service.addDriver(new Driver("D1", 5));

        AtomicInteger matched = new AtomicInteger();
        Runnable rider = () -> {
            if (service.requestRide(Thread.currentThread().getName(), 5) != null) {
                matched.incrementAndGet();
            }
        };
        Thread[] ts = new Thread[6];
        for (int i = 0; i < ts.length; i++) {
            ts[i] = new Thread(rider, "rider-" + i);
            ts[i].start();
        }
        for (Thread t : ts) {
            t.join();
        }
        System.out.println("Riders matched to the single driver (must be exactly 1): " + matched.get());
    }
}
