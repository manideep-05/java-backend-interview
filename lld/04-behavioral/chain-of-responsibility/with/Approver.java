package with;

/**
 * CHAIN OF RESPONSIBILITY APPLIED — the base handler.
 *
 * Each handler knows its own approval limit and a reference to the NEXT
 * handler. If it can't approve, it forwards. The template handle()
 * method encodes the "handle or pass" logic once.
 */
public abstract class Approver {

    private Approver next;
    private final double limit;
    private final String title;

    protected Approver(String title, double limit) {
        this.title = title;
        this.limit = limit;
    }

    /** Link the next handler and return it, enabling fluent chaining. */
    public Approver linkTo(Approver next) {
        this.next = next;
        return next;
    }

    public void handle(double amount) {
        if (amount <= limit) {
            System.out.println(title + " approved $" + amount);
        } else if (next != null) {
            next.handle(amount); // pass it along
        } else {
            System.out.println("Rejected: $" + amount + " exceeds all limits");
        }
    }
}
