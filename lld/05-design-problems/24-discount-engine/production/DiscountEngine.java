package production;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PRODUCTION engine.
 *
 * Concurrency design:
 *  - discounts is a CopyOnWriteArrayList: configured rarely, iterated on
 *    every pricing call from many checkout threads -> lock-free reads.
 *  - The engine holds NO per-request mutable state; each apply() works on
 *    local variables + the (thread-safe) discount objects, so many
 *    checkouts price concurrently with no contention.
 *  - Single-use coupon safety lives inside CouponDiscount (atomic set),
 *    not in the engine — keeping the engine stateless.
 */
public class DiscountEngine {
    private final List<Discount> discounts = new CopyOnWriteArrayList<>();

    public void register(Discount discount) {
        discounts.add(discount);
    }

    public long apply(Cart cart) {
        long total = cart.getSubtotalCents();
        for (Discount d : discounts) {
            if (d.isApplicable(cart)) {
                total = Math.max(0, total - d.computeDiscount(total, cart));
            }
        }
        return total;
    }

    public static void main(String[] args) throws InterruptedException {
        DiscountEngine engine = new DiscountEngine();
        CouponDiscount welcome = new CouponDiscount("WELCOME", 1000); // $10 off, once
        engine.register(welcome);

        AtomicInteger discounted = new AtomicInteger();
        // 8 concurrent checkouts try to use the SAME one-time coupon.
        Runnable checkout = () -> {
            long before = 5000;
            long after = engine.apply(new Cart(before, "any"));
            if (after < before) {
                discounted.incrementAndGet(); // got the coupon
            }
        };
        Thread[] ts = new Thread[8];
        for (int i = 0; i < ts.length; i++) {
            ts[i] = new Thread(checkout);
            ts[i].start();
        }
        for (Thread t : ts) {
            t.join();
        }
        System.out.println("Checkouts that redeemed the one-time coupon (must be 1): " + discounted.get());
    }
}
