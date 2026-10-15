package normal;

/** A concrete payment method. */
public class CardPayment implements PaymentStrategy {
    @Override
    public boolean charge(long amountCents) {
        System.out.println("Charged card: " + amountCents + " cents");
        return true;
    }
}
