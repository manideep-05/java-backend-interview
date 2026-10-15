package without;

/**
 * STRATEGY NOT USED — the problem.
 *
 * The algorithm is selected by a switch inside pay(). Every new payment
 * method edits this method, risking the existing branches, and the
 * class accumulates unrelated logic.
 */
public class PaymentContext {

    public void pay(String type, double amount) {
        switch (type) {
            case "CARD":
                System.out.println("Paid $" + amount + " by card (with 3-D Secure)");
                break;
            case "PAYPAL":
                System.out.println("Paid $" + amount + " via PayPal");
                break;
            default:
                throw new IllegalArgumentException("Unknown: " + type);
        }
    }

    public static void main(String[] args) {
        PaymentContext ctx = new PaymentContext();
        ctx.pay("CARD", 100);
        ctx.pay("PAYPAL", 40);
    }
}
