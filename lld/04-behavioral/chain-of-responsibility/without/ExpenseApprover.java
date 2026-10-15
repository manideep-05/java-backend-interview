package without;

/**
 * CHAIN OF RESPONSIBILITY NOT USED — the problem.
 *
 * A single method decides the approver with nested if/else on amount.
 * Thresholds and tiers are hard-coded together; adding a tier or
 * reordering levels means rewriting this method.
 */
public class ExpenseApprover {

    public void approve(double amount) {
        if (amount <= 1000) {
            System.out.println("Team Lead approved $" + amount);
        } else if (amount <= 5000) {
            System.out.println("Manager approved $" + amount);
        } else if (amount <= 20000) {
            System.out.println("Director approved $" + amount);
        } else {
            System.out.println("Rejected: $" + amount + " exceeds all limits");
        }
    }

    public static void main(String[] args) {
        ExpenseApprover approver = new ExpenseApprover();
        approver.approve(500);
        approver.approve(3000);
        approver.approve(15000);
    }
}
