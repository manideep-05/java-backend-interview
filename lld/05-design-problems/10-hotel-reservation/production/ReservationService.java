package production;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * PRODUCTION facade.
 *
 * Concurrency design — PER-ROOM (striped) locking:
 *  - The dangerous compound action is "check no overlap, then insert"
 *    for a given room. We lock a per-room monitor object during that
 *    whole action, so two threads can't both pass the overlap check for
 *    the SAME room and double-book it.
 *  - Locks are striped by room: distinct rooms use distinct lock objects,
 *    so reservations on different rooms proceed fully in parallel. A
 *    single global lock would needlessly serialize the whole hotel.
 *  - bookings/locks are ConcurrentHashMaps; the per-room list is a
 *    CopyOnWriteArrayList (reads during availability search are lock-free;
 *    writes happen under the room lock anyway).
 */
public class ReservationService {
    private final Map<String, List<Reservation>> bookings = new ConcurrentHashMap<>();
    private final Map<String, Object> roomLocks = new ConcurrentHashMap<>();

    private Object lockFor(String roomId) {
        // computeIfAbsent gives a stable, unique lock object per room.
        return roomLocks.computeIfAbsent(roomId, k -> new Object());
    }

    public Reservation reserve(String roomId, String guest, DateRange range) {
        synchronized (lockFor(roomId)) { // guard check-then-insert for THIS room
            List<Reservation> existing = bookings.getOrDefault(roomId, List.of());
            for (Reservation r : existing) {
                if (r.getRange().overlaps(range)) {
                    return null; // overlap -> reject
                }
            }
            Reservation reservation = new Reservation(roomId, guest, range);
            bookings.computeIfAbsent(roomId, k -> new CopyOnWriteArrayList<>()).add(reservation);
            return reservation;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        ReservationService service = new ReservationService();
        DateRange sameRange = new DateRange(
                java.time.LocalDate.of(2026, 1, 1), java.time.LocalDate.of(2026, 1, 5));

        java.util.concurrent.atomic.AtomicInteger winners = new java.util.concurrent.atomic.AtomicInteger();

        // 8 guests race for room 101 on the SAME overlapping range; exactly one wins.
        Runnable attempt = () -> {
            if (service.reserve("101", Thread.currentThread().getName(), sameRange) != null) {
                winners.incrementAndGet();
            }
        };
        Thread[] ts = new Thread[8];
        for (int i = 0; i < ts.length; i++) {
            ts[i] = new Thread(attempt, "guest-" + i);
            ts[i].start();
        }
        for (Thread t : ts) {
            t.join();
        }
        System.out.println("Winners for room 101 (must be exactly 1): " + winners.get());
    }
}
