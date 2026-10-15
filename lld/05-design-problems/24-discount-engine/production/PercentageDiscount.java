package production;

/** Stateless => thread-safe to share. */
public final class PercentageDiscount implements Discount {
    private final int percent;
    private final long minCents;

    public PercentageDiscount(int percent, long minCents) {
        this.percent = percent;
        this.minCents = minCents;
    }

    @Override public boolean isApplicable(Cart cart) { return cart.getSubtotalCents() >= minCents; }
    @Override public long computeDiscount(long currentTotalCents, Cart cart) { return currentTotalCents * percent / 100; }
    @Override public String name() { return percent + "% off"; }
}
