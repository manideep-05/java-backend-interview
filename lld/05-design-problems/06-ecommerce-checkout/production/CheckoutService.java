package production;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PRODUCTION facade.
 *
 * Concurrency + correctness design:
 *  - Stock reserved via atomic CAS BEFORE charging (reserve-then-charge),
 *    so we never charge for something we can't fulfill.
 *  - Payment uses an idempotency key = the order id, so a retried
 *    checkout for the same order won't double-charge.
 *  - On charge failure, compensate: release the reserved stock.
 *  - No service-wide lock: the only shared mutable state (inventory,
 *    payment-seen map) is internally atomic, so distinct checkouts don't
 *    block each other.
 */
public class CheckoutService {
    private final Inventory inventory;
    private final PaymentStrategy payment;

    public CheckoutService(Inventory inventory, PaymentStrategy payment) {
        this.inventory = inventory;
        this.payment = payment;
    }

    public boolean checkout(String orderId, String product, int qty, long amountCents) {
        // 1) Atomic reserve (anti-oversell).
        if (!inventory.reserve(product, qty)) {
            return false;
        }
        // 2) Idempotent charge keyed by order id.
        if (!payment.charge(amountCents, orderId)) {
            inventory.release(product, qty); // compensate
            return false;
        }
        // 3) Confirmed.
        return true;
    }

    public static void main(String[] args) throws InterruptedException {
        Inventory inv = new Inventory();
        inv.set("gpu", 1); // exactly ONE in stock
        CheckoutService service = new CheckoutService(inv, new CardPayment());

        AtomicInteger confirmed = new AtomicInteger();
        // 10 shoppers race for the single GPU; exactly one should win.
        Runnable shopper = () -> {
            String orderId = UUID.randomUUID().toString();
            if (service.checkout(orderId, "gpu", 1, 50000)) {
                confirmed.incrementAndGet();
            }
        };
        Thread[] ts = new Thread[10];
        for (int i = 0; i < ts.length; i++) {
            ts[i] = new Thread(shopper);
            ts[i].start();
        }
        for (Thread t : ts) {
            t.join();
        }
        System.out.println("Confirmed orders (must be exactly 1): " + confirmed.get()
                + ", remaining stock: " + inv.available("gpu"));
    }
}
