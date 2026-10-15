package normal;

import java.time.LocalDate;

/**
 * A half-open date interval [checkIn, checkOut). Half-open means a
 * checkout on day X does NOT conflict with a check-in on day X — so
 * back-to-back bookings are allowed. Responsibility: interval math.
 */
public class DateRange {
    private final LocalDate checkIn;
    private final LocalDate checkOut;

    public DateRange(LocalDate checkIn, LocalDate checkOut) {
        if (!checkIn.isBefore(checkOut)) {
            throw new IllegalArgumentException("checkIn must be before checkOut");
        }
        this.checkIn = checkIn;
        this.checkOut = checkOut;
    }

    /** [a,b) overlaps [c,d) iff a < d && c < b. */
    public boolean overlaps(DateRange other) {
        return this.checkIn.isBefore(other.checkOut) && other.checkIn.isBefore(this.checkOut);
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }
}
