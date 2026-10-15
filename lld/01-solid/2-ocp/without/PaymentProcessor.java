package without;

/**
 * OCP VIOLATION.
 *
 * Every new payment method forces you to EDIT this class: add a new
 * branch to the switch. That means re-testing credit-card and paypal
 * logic every time you add, say, UPI — even though they didn't change.
 *
 * The class is NOT closed for modification.
 */
public class PaymentProcessor {

    public void pay(String type, double amount) {
        switch (type) {
            case "CREDIT_CARD":
                System.out.println("Paid $" + amount + " via Credit Card");
                break;
            case "PAYPAL":
                System.out.println("Paid $" + amount + " via PayPal");
                break;
            // To add UPI we must OPEN this file and MODIFY it:
            // case "UPI": ...
            default:
                throw new IllegalArgumentException("Unknown payment type: " + type);
        }
    }

    public static void main(String[] args) {
        PaymentProcessor p = new PaymentProcessor();
        p.pay("CREDIT_CARD", 100);
        p.pay("PAYPAL", 50);
    }
}
