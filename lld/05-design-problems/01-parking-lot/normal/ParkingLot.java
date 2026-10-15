package normal;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FACADE / orchestrator for the whole subsystem.
 *
 * It wires levels + strategies together and exposes the two use cases
 * (park, unpark). It does NOT compute pricing or choose spots itself —
 * those are delegated to strategies (SRP, DIP).
 */
public class ParkingLot {
    private final List<Level> levels = new ArrayList<>();
    private final PricingStrategy pricingStrategy;
    // Active tickets keyed by id, so unpark can look one up.
    private final Map<String, Ticket> activeTickets = new ConcurrentHashMap<>();

    public ParkingLot(PricingStrategy pricingStrategy) {
        this.pricingStrategy = pricingStrategy;
    }

    public void addLevel(Level level) {
        levels.add(level);
    }

    /** Park flow: find a spot on some level, issue a ticket. */
    public Ticket park(Vehicle vehicle) {
        for (Level level : levels) {
            ParkingSpot spot = level.parkVehicle(vehicle);
            if (spot != null) {
                Ticket ticket = new Ticket(UUID.randomUUID().toString(), vehicle, spot, Instant.now());
                activeTickets.put(ticket.getId(), ticket);
                return ticket;
            }
        }
        throw new IllegalStateException("Parking full for vehicle type: " + vehicle.getType());
    }

    /** Unpark flow: settle fee, free the spot, close the ticket. */
    public double unpark(String ticketId) {
        Ticket ticket = activeTickets.get(ticketId);
        if (ticket == null || ticket.isClosed()) {
            throw new IllegalArgumentException("Invalid or already-closed ticket: " + ticketId);
        }
        ticket.close(Instant.now());
        double fee = pricingStrategy.calculate(ticket);
        ticket.getSpot().free();
        activeTickets.remove(ticketId);
        return fee;
    }

    public static void main(String[] args) {
        ParkingLot lot = new ParkingLot(new HourlyPricingStrategy(10.0));
        Level level = new Level(1, new FirstFitStrategy());
        level.addSpot(new ParkingSpot("1-A1", SpotType.COMPACT));
        level.addSpot(new ParkingSpot("1-A2", SpotType.LARGE));
        lot.addLevel(level);

        Vehicle car = new Vehicle("KA-01-1234", VehicleType.CAR);
        Ticket ticket = lot.park(car);
        System.out.println("Parked at spot " + ticket.getSpot().getId());

        double fee = lot.unpark(ticket.getId());
        System.out.println("Fee charged: $" + fee);
    }
}
