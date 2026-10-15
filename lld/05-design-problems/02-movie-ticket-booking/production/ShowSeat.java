package production;

import java.util.concurrent.atomic.AtomicReference;

/**
 * PRODUCTION show-seat.
 *
 * The status is an AtomicReference<SeatStatus>. tryHold() CAS-flips
 * AVAILABLE -> HELD; exactly one of two racing users wins, so the same
 * seat can never be double-held. We also store a holdExpiry so an
 * abandoned hold can be reclaimed.
 *
 * Expiry handling is LAZY: tryHold() first checks whether an existing
 * HELD seat is past its expiry and, if so, CAS-reclaims it to AVAILABLE
 * before competing for it. (A background sweeper could do the same.)
 */
public class ShowSeat {
    private final String seatId;
    private final AtomicReference<SeatStatus> status = new AtomicReference<>(SeatStatus.AVAILABLE);
    private volatile String heldBy;
    private volatile long holdExpiryMillis;

    public ShowSeat(String seatId) {
        this.seatId = seatId;
    }

    /** Atomically hold the seat for `holdMillis`. Returns true if won. */
    public boolean tryHold(String user, long holdMillis) {
        // Reclaim an expired hold first (lazy expiry).
        if (status.get() == SeatStatus.HELD && System.currentTimeMillis() > holdExpiryMillis) {
            status.compareAndSet(SeatStatus.HELD, SeatStatus.AVAILABLE);
        }
        if (status.compareAndSet(SeatStatus.AVAILABLE, SeatStatus.HELD)) {
            heldBy = user;
            holdExpiryMillis = System.currentTimeMillis() + holdMillis;
            return true;
        }
        return false;
    }

    /** Idempotent-ish confirm: only the holder, only from HELD. */
    public boolean confirm(String user) {
        if (user.equals(heldBy) && status.compareAndSet(SeatStatus.HELD, SeatStatus.BOOKED)) {
            return true;
        }
        return false;
    }

    public void release() {
        heldBy = null;
        // Only release if we currently hold it (don't stomp a BOOKED seat).
        status.compareAndSet(SeatStatus.HELD, SeatStatus.AVAILABLE);
    }

    public SeatStatus getStatus() {
        return status.get();
    }

    public String getSeatId() {
        return seatId;
    }
}
