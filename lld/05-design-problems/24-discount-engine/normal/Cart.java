package normal;

/**
 * A minimal cart: a subtotal in cents and a category tag used by
 * eligibility rules. (Line items omitted for focus on the discount logic.)
 */
public class Cart {
    private final long subtotalCents;
    private final String category;

    public Cart(long subtotalCents, String category) {
        this.subtotalCents = subtotalCents;
        this.category = category;
    }

    public long getSubtotalCents() { return subtotalCents; }
    public String getCategory() { return category; }
}
