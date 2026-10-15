package normal;

/**
 * STRATEGY: a payment method. Swapping card for wallet is a new
 * implementation, no change to the checkout flow (OCP, DIP).
 */
public interface PaymentStrategy {
    boolean charge(long amountCents);
}
