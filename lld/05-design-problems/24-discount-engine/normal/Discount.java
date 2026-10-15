package normal;

/**
 * STRATEGY + SPECIFICATION.
 *
 * isApplicable(cart) is the eligibility rule (specification); computeDiscount
 * returns how many cents to take off the CURRENT running total. A new
 * discount type = a new class; the engine never changes (OCP).
 */
public interface Discount {
    boolean isApplicable(Cart cart);
    long computeDiscount(long currentTotalCents, Cart cart);
    String name();
}
