package normal;

/**
 * Order with a lifecycle state. Transition methods enforce legal moves
 * (you can't confirm an unpaid order) — the behavioral core of the
 * State idea, expressed compactly with an enum + guards.
 */
public class Order {
    private final String id;
    private final String product;
    private final int qty;
    private final long amountCents;
    private OrderState state = OrderState.CREATED;

    public Order(String id, String product, int qty, long amountCents) {
        this.id = id;
        this.product = product;
        this.qty = qty;
        this.amountCents = amountCents;
    }

    public void markPaid() {
        if (state != OrderState.CREATED) {
            throw new IllegalStateException("Can only pay a CREATED order");
        }
        state = OrderState.PAID;
    }

    public void confirm() {
        if (state != OrderState.PAID) {
            throw new IllegalStateException("Can only confirm a PAID order");
        }
        state = OrderState.CONFIRMED;
    }

    public void cancel() {
        state = OrderState.CANCELLED;
    }

    public OrderState getState() { return state; }
    public String getProduct() { return product; }
    public int getQty() { return qty; }
    public long getAmountCents() { return amountCents; }
    public String getId() { return id; }
}
