package production;

/**
 * STRATEGY: payment method with an IDEMPOTENCY KEY.
 *
 * The key lets the provider recognize a retried charge and return the
 * original outcome instead of charging twice — essential because clients
 * and networks retry.
 */
public interface PaymentStrategy {
    boolean charge(long amountCents, String idempotencyKey);
}
