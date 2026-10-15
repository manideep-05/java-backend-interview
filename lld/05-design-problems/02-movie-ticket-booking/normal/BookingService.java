package normal;

import java.util.ArrayList;
import java.util.List;

/**
 * FACADE orchestrating hold -> confirm.
 *
 * The important bit is ALL-OR-NOTHING holding: if any requested seat
 * can't be held, we release the ones already held and fail the whole
 * request (no partial bookings).
 */
public class BookingService {

    /** Try to hold all requested seats atomically (all-or-nothing). */
    public boolean hold(Show show, List<String> seatIds, String user) {
        List<ShowSeat> grabbed = new ArrayList<>();
        for (String seatId : seatIds) {
            ShowSeat seat = show.getSeat(seatId);
            if (seat != null && seat.hold(user)) {
                grabbed.add(seat);
            } else {
                // Roll back everything grabbed so far, then fail.
                grabbed.forEach(ShowSeat::release);
                System.out.println("Hold failed: seat " + seatId + " unavailable. Rolled back.");
                return false;
            }
        }
        System.out.println(user + " holds " + seatIds);
        return true;
    }

    public boolean confirm(Show show, List<String> seatIds, String user) {
        for (String seatId : seatIds) {
            if (!show.getSeat(seatId).confirm(user)) {
                System.out.println("Confirm failed for seat " + seatId);
                return false;
            }
        }
        System.out.println(user + " booked " + seatIds);
        return true;
    }

    public static void main(String[] args) {
        Show show = new Show("SH1", "Dune", 10);
        BookingService service = new BookingService();

        List<String> seats = List.of("S1", "S2");
        if (service.hold(show, seats, "alice")) {
            service.confirm(show, seats, "alice"); // booked
        }
        // bob tries the same seats -> hold fails (already booked)
        service.hold(show, seats, "bob");
    }
}
