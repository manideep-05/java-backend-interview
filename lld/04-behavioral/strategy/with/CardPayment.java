package with;

/** A concrete strategy. */
public class CardPayment implements PaymentStrategy {
    @Override
    public void pay(double amount) {
        System.out.println("Paid $" + amount + " by card (with 3-D Secure)");
    }
}
