package normal;

/**
 * Order with a guarded lifecycle. Each transition validates the current
 * state, so illegal moves (deliver before pickup) are rejected — the
 * behavioral core of the State idea in compact enum form.
 */
public class Order {
    private final String id;
    private final String customer;
    private OrderState state = OrderState.PLACED;
    private String agentId;

    public Order(String id, String customer) {
        this.id = id;
        this.customer = customer;
    }

    public void accept() { require(OrderState.PLACED); state = OrderState.ACCEPTED; }
    public void ready() { require(OrderState.ACCEPTED, OrderState.PREPARING); state = OrderState.READY; }

    public void assign(String agentId) {
        require(OrderState.READY);
        this.agentId = agentId;
        state = OrderState.ASSIGNED;
    }

    public void pickUp() { require(OrderState.ASSIGNED); state = OrderState.PICKED_UP; }
    public void deliver() { require(OrderState.PICKED_UP); state = OrderState.DELIVERED; }

    public void cancel() {
        if (state == OrderState.PICKED_UP || state == OrderState.DELIVERED) {
            throw new IllegalStateException("Too late to cancel");
        }
        state = OrderState.CANCELLED;
    }

    private void require(OrderState... allowed) {
        for (OrderState s : allowed) {
            if (state == s) {
                return;
            }
        }
        throw new IllegalStateException("Illegal transition from " + state);
    }

    public OrderState getState() { return state; }
    public String getAgentId() { return agentId; }
    public String getId() { return id; }
    public String getCustomer() { return customer; }
}
