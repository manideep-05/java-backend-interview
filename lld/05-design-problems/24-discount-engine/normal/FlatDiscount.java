package normal;

/** A flat amount off (e.g. $5 off), never more than the current total. */
public class FlatDiscount implements Discount {
    private final long offCents;

    public FlatDiscount(long offCents) {
        this.offCents = offCents;
    }

    @Override
    public boolean isApplicable(Cart cart) {
        return true;
    }

    @Override
    public long computeDiscount(long currentTotalCents, Cart cart) {
        return Math.min(offCents, currentTotalCents); // can't exceed the total
    }

    @Override
    public String name() { return (offCents / 100.0) + " flat off"; }
}
