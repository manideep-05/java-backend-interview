package production;

import java.time.Duration;

/**
 * Hourly pricing. Stateless and immutable (ratePerHour is final), so a
 * single instance is safe to share across all gates/threads.
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
            hours = 1;
        }
        return hours * ratePerHour;
    }
}
