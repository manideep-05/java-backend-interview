package with;

/**
 * OCP APPLIED — the abstraction.
 *
 * This interface is the stable "closed" part. The PaymentProcessor
 * depends only on this. New payment methods extend the system by
 * IMPLEMENTING this interface — no existing code is modified.
 */
public interface PaymentMethod {
    void pay(double amount);
}
