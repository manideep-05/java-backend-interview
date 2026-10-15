package normal;

/**
 * STRATEGY pattern — the pricing policy.
 *
 * Pricing changes independently of parking mechanics (hourly, flat,
 * weekend surge). Isolating it here means a new price model is a new
 * class, not an edit to the lot (SRP + OCP).
 */
public interface PricingStrategy {
    double calculate(Ticket ticket);
}
