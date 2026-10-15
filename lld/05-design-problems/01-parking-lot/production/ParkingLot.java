package production;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * PRODUCTION facade.
 *
 * Thread-safety summary:
 *  - levels: CopyOnWriteArrayList (configured once, iterated often).
 *  - activeTickets: ConcurrentHashMap for safe concurrent put/get/remove.
 *  - The park() scan is lock-free; correctness for the "last spot" race is
 *    guaranteed by ParkingSpot's CAS claim, NOT by a global lock. This
 *    keeps throughput high: cars on different levels/spots never block
 *    each other.
 *  - unpark() uses the ticket's atomic tryClose() so a re-presented ticket
 *    is idempotent (no double charge, no double free).
 */
public class ParkingLot {
    private final List<Level> levels = new CopyOnWriteArrayList<>();
    private final PricingStrategy pricingStrategy;
    private final Map<String, Ticket> activeTickets = new ConcurrentHashMap<>();

    public ParkingLot(PricingStrategy pricingStrategy) {
        this.pricingStrategy = pricingStrategy;
    }

    public void addLevel(Level level) {
        levels.add(level);
    }

    public Ticket park(Vehicle vehicle) {
        for (Level level : levels) {
            ParkingSpot spot = level.tryPark(vehicle);
            if (spot != null) {
                Ticket ticket = new Ticket(UUID.randomUUID().toString(), vehicle, spot, Instant.now());
                activeTickets.put(ticket.getId(), ticket);
                return ticket;
            }
        }
        throw new IllegalStateException("Parking full for vehicle type: " + vehicle.getType());
    }

    public double unpark(String ticketId) {
        Ticket ticket = activeTickets.get(ticketId);
        if (ticket == null) {
            throw new IllegalArgumentException("Unknown ticket: " + ticketId);
        }
        // Only the first caller to close the ticket proceeds; a concurrent
        // or repeated exit on the same ticket is rejected idempotently.
        if (!ticket.tryClose(Instant.now())) {
            throw new IllegalStateException("Ticket already settled: " + ticketId);
        }
        double fee = pricingStrategy.calculate(ticket);
        // Free the spot through its owning level so the level's availability
        // counter is incremented back in step with the free.
        ParkingSpot spot = ticket.getSpot();
        spot.getLevel().release(spot);
        activeTickets.remove(ticketId);
        return fee;
    }

    public static void main(String[] args) throws InterruptedException {
        ParkingLot lot = new ParkingLot(new HourlyPricingStrategy(10.0));
        Level level = new Level(1);
        // Exactly ONE large spot — so concurrent parkers must contend for it.
        level.addSpot(new ParkingSpot("1-A1", SpotType.LARGE));
        lot.addLevel(level);

        // Simulate two cars racing for the single spot.
        Runnable attempt = () -> {
            try {
                Ticket t = lot.park(new Vehicle(Thread.currentThread().getName(), VehicleType.CAR));
                System.out.println(Thread.currentThread().getName() + " WON spot " + t.getSpot().getId());
            } catch (IllegalStateException e) {
                System.out.println(Thread.currentThread().getName() + " rejected: " + e.getMessage());
            }
        };
        Thread a = new Thread(attempt, "car-A");
        Thread b = new Thread(attempt, "car-B");
        a.start(); b.start();
        a.join(); b.join();
        // Exactly one of car-A / car-B wins; the other is rejected. No double-assign.
    }
}
