package normal;

import java.util.ArrayList;
import java.util.List;

/**
 * The engine. Runs discounts as an ordered PIPELINE (stacking): each
 * applicable discount reduces the running total, and we clamp at zero so a
 * discount can never make the total negative. Depends only on the Discount
 * abstraction — new types need no change here (OCP).
 */
public class DiscountEngine {
    private final List<Discount> discounts = new ArrayList<>();

    public void register(Discount discount) {
        discounts.add(discount); // order defines stacking order
    }

    public long apply(Cart cart) {
        long total = cart.getSubtotalCents();
        for (Discount d : discounts) {
            if (d.isApplicable(cart)) {
                long off = d.computeDiscount(total, cart);
                total = Math.max(0, total - off); // floor at zero
                System.out.println("applied " + d.name() + ", -" + off + " -> " + total);
            }
        }
        return total;
    }

    public static void main(String[] args) {
        DiscountEngine engine = new DiscountEngine();
        engine.register(new PercentageDiscount(10, 5000)); // 10% off if >= $50
        engine.register(new FlatDiscount(500));            // then $5 flat off

        Cart cart = new Cart(10000, "electronics"); // $100
        long finalTotal = engine.apply(cart);
        System.out.println("Final total: " + finalTotal + " cents"); // 100 -> 90 -> 85 => 8500
    }
}
