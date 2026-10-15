package with;

/**
 * The chain is assembled by linking handlers. Adding a new tier (e.g. a
 * VP) means writing one class and inserting one link — no handler's
 * internal logic changes.
 */
public class Main {
    public static void main(String[] args) {
        Approver teamLead = new TeamLead();
        teamLead.linkTo(new Manager()).linkTo(new Director());

        teamLead.handle(500);   // Team Lead
        teamLead.handle(3000);  // Manager
        teamLead.handle(15000); // Director
        teamLead.handle(50000); // Rejected
    }
}
