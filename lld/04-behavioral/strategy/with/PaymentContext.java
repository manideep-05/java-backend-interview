package with;

/**
 * STRATEGY APPLIED — the context.
 *
 * Holds a PaymentStrategy and delegates to it. No switch; the algorithm
 * can be swapped at runtime. Adding a new method = adding a new strategy
 * class, with zero edits here.
 */
public class PaymentContext {

    private PaymentStrategy strategy;

    public PaymentContext(PaymentStrategy strategy) {
        this.strategy = strategy;
    }

    // Strategy can also be changed at runtime.
    public void setStrategy(PaymentStrategy strategy) {
        this.strategy = strategy;
    }

    public void pay(double amount) {
        strategy.pay(amount);
    }

    public static void main(String[] args) {
        PaymentContext ctx = new PaymentContext(new CardPayment());
        ctx.pay(100);

        ctx.setStrategy(new PayPalPayment()); // swap at runtime
        ctx.pay(40);
    }
}
