package normal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FACADE. Keeps bookings per room and offers search / reserve / cancel.
 *
 * reserve() = "check no overlap, then insert". Single-threaded here, so
 * the check-then-insert gap is harmless; production guards it per room.
 */
public class ReservationService {
    private final Map<String, List<Reservation>> bookings = new HashMap<>();

    public boolean isAvailable(String roomId, DateRange range) {
        for (Reservation r : bookings.getOrDefault(roomId, List.of())) {
            if (r.getRange().overlaps(range)) {
                return false;
            }
        }
        return true;
    }

    public Reservation reserve(String roomId, String guest, DateRange range) {
        if (!isAvailable(roomId, range)) {
            System.out.println("Room " + roomId + " not available for that range.");
            return null;
        }
        Reservation reservation = new Reservation(roomId, guest, range);
        bookings.computeIfAbsent(roomId, k -> new ArrayList<>()).add(reservation);
        System.out.println(guest + " reserved " + roomId);
        return reservation;
    }

    public static void main(String[] args) {
        ReservationService service = new ReservationService();
        DateRange r1 = new DateRange(java.time.LocalDate.of(2026, 1, 1), java.time.LocalDate.of(2026, 1, 5));
        DateRange r2 = new DateRange(java.time.LocalDate.of(2026, 1, 4), java.time.LocalDate.of(2026, 1, 8));
        DateRange r3 = new DateRange(java.time.LocalDate.of(2026, 1, 5), java.time.LocalDate.of(2026, 1, 9));

        service.reserve("101", "alice", r1); // ok
        service.reserve("101", "bob", r2);   // overlaps -> rejected
        service.reserve("101", "carol", r3); // back-to-back (starts on alice's checkout) -> ok
    }
}
