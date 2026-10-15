package normal;

import java.util.UUID;

/**
 * FACADE orchestrating the checkout flow.
 *
 * ORDER OF OPERATIONS MATTERS: reserve stock BEFORE charging, so we never
 * take money we can't fulfill. If the charge fails, release the stock
 * (compensation) and cancel the order.
 */
public class CheckoutService {
    private final Inventory inventory;
    private final PaymentStrategy payment;

    public CheckoutService(Inventory inventory, PaymentStrategy payment) {
        this.inventory = inventory;
        this.payment = payment;
    }

    public Order checkout(String product, int qty, long amountCents) {
        Order order = new Order(UUID.randomUUID().toString(), product, qty, amountCents);

        // 1) Reserve stock first.
        if (!inventory.reserve(product, qty)) {
            order.cancel();
            System.out.println("Out of stock: " + product);
            return order;
        }
        // 2) Charge. On failure, release stock (compensate).
        if (!payment.charge(amountCents)) {
            inventory.release(product, qty);
            order.cancel();
            System.out.println("Payment failed; stock released.");
            return order;
        }
        // 3) Finalize state.
        order.markPaid();
        order.confirm();
        System.out.println("Order " + order.getId() + " CONFIRMED");
        return order;
    }

    public static void main(String[] args) {
        Inventory inv = new Inventory();
        inv.set("book", 1);
        CheckoutService service = new CheckoutService(inv, new CardPayment());

        service.checkout("book", 1, 1999); // confirmed
        service.checkout("book", 1, 1999); // out of stock
    }
}
