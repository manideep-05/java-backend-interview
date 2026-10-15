package with;

/**
 * OCP APPLIED — the stable, "closed" processor.
 *
 * It depends on the PaymentMethod abstraction, not on concrete types.
 * No switch, no if/else on type. It will never need editing when a
 * new payment method is introduced.
 */
public class PaymentProcessor {

    public void process(PaymentMethod method, double amount) {
        method.pay(amount);
    }

    public static void main(String[] args) {
        PaymentProcessor processor = new PaymentProcessor();

        processor.process(new CreditCardPayment(), 100);
        processor.process(new PayPalPayment(), 50);
        processor.process(new UpiPayment(), 25); // new method, no processor change
    }
}
