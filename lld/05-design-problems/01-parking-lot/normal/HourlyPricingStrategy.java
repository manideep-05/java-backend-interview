package normal;

import java.time.Duration;

/**
 * A concrete pricing policy: a flat rate per hour, rounding partial
 * hours UP (a stated, explicit policy — a common interview edge case).
 */
public class HourlyPricingStrategy implements PricingStrategy {
    private final double ratePerHour;

    public HourlyPricingStrategy(double ratePerHour) {
        this.ratePerHour = ratePerHour;
    }

    @Override
    public double calculate(Ticket ticket) {
        Duration duration = Duration.between(ticket.getEntryTime(), ticket.getExitTime());
        long hours = (long) Math.ceil(duration.toMinutes() / 60.0);
        if (hours == 0) {
            hours = 1; // minimum one hour
        }
        return hours * ratePerHour;
    }
}
