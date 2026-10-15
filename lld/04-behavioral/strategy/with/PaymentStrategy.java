package with;

/** The strategy interface — the common shape of every algorithm variant. */
public interface PaymentStrategy {
    void pay(double amount);
}
