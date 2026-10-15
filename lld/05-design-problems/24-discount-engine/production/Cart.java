package production;

/** Immutable cart snapshot (safe to share across threads during pricing). */
public final class Cart {
    private final long subtotalCents;
    private final String category;

    public Cart(long subtotalCents, String category) {
        this.subtotalCents = subtotalCents;
        this.category = category;
    }

    public long getSubtotalCents() { return subtotalCents; }
    public String getCategory() { return category; }
}
