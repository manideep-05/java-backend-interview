package normal;

/**
 * A seat WITHIN a show. Status lives here (not on a physical Seat)
 * because the same seat has different status per show.
 *
 * NOTE (normal): hold() is check-then-set — fine single-threaded; the
 * production version makes the transition atomic.
 */
public class ShowSeat {
    private final String seatId;
    private SeatStatus status = SeatStatus.AVAILABLE;
    private String heldBy;

    public ShowSeat(String seatId) {
        this.seatId = seatId;
    }

    public boolean hold(String user) {
        if (status == SeatStatus.AVAILABLE) {
            status = SeatStatus.HELD;
            heldBy = user;
            return true;
        }
        return false;
    }

    public boolean confirm(String user) {
        if (status == SeatStatus.HELD && user.equals(heldBy)) {
            status = SeatStatus.BOOKED;
            return true;
        }
        return false;
    }

    public void release() {
        status = SeatStatus.AVAILABLE;
        heldBy = null;
    }

    public SeatStatus getStatus() {
        return status;
    }

    public String getSeatId() {
        return seatId;
    }
}
