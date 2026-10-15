package with;

/** OCP APPLIED — one concrete extension. */
public class CreditCardPayment implements PaymentMethod {
    @Override
    public void pay(double amount) {
        System.out.println("Paid $" + amount + " via Credit Card");
    }
}
