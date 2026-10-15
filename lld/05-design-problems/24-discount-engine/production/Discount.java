package production;

/** STRATEGY + SPECIFICATION (same contract as normal). Implementations
 *  must be stateless so one instance is safe to share across threads. */
public interface Discount {
    boolean isApplicable(Cart cart);
    long computeDiscount(long currentTotalCents, Cart cart);
    String name();
}
