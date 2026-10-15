package with;

/**
 * OCP APPLIED — the NEW requirement.
 *
 * Adding UPI support required ZERO edits to PaymentProcessor,
 * CreditCardPayment, or PayPalPayment. We only ADDED this file.
 * That is "open for extension, closed for modification" in action.
 */
public class UpiPayment implements PaymentMethod {
    @Override
    public void pay(double amount) {
        System.out.println("Paid $" + amount + " via UPI");
    }
}
