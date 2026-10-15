package production;

/** STRATEGY — pricing policy (stateless, so inherently thread-safe to share). */
public interface PricingStrategy {
    double calculate(Ticket ticket);
}
