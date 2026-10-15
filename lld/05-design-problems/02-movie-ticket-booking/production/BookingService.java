package production;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PRODUCTION facade.
 *
 * Concurrency design:
 *  - No service-level lock: correctness comes from each ShowSeat's atomic
 *    CAS transition. Different seats never contend; the same seat is
 *    arbitrated by CAS. This maximizes concurrency on a hot show.
 *  - All-or-nothing hold: grab seats via CAS; if any fails, release the
 *    ones we grabbed (CAS back) and fail. No partial bookings.
 *  - Holds carry an expiry so abandoned carts self-heal (lazy reclaim in
 *    ShowSeat.tryHold).
 */
public class BookingService {

    private static final long HOLD_MILLIS = 2 * 60 * 1000; // 2 minutes

    public boolean hold(Show show, List<String> seatIds, String user) {
        List<ShowSeat> grabbed = new ArrayList<>();
        for (String seatId : seatIds) {
            ShowSeat seat = show.getSeat(seatId);
            if (seat != null && seat.tryHold(user, HOLD_MILLIS)) {
                grabbed.add(seat);
            } else {
                grabbed.forEach(ShowSeat::release); // roll back
                return false;
            }
        }
        return true;
    }

    public boolean confirm(Show show, List<String> seatIds, String user) {
        for (String seatId : seatIds) {
            if (!show.getSeat(seatId).confirm(user)) {
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args) throws InterruptedException {
        Show show = new Show("SH1", "Dune", 10);
        BookingService service = new BookingService();
        List<String> seats = List.of("S1", "S2");

        AtomicInteger winners = new AtomicInteger();

        // 5 users race for the SAME two seats; exactly one should win.
        Runnable attempt = () -> {
            String user = Thread.currentThread().getName();
            if (service.hold(show, seats, user)) {
                winners.incrementAndGet();
                System.out.println(user + " WON the seats");
            } else {
                System.out.println(user + " lost the race");
            }
        };
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Thread t = new Thread(attempt, "user-" + i);
            threads.add(t);
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }
        System.out.println("Winners (must be exactly 1): " + winners.get());
    }
}
