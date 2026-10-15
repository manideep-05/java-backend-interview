package without;

/**
 * STATE NOT USED — the problem.
 *
 * Every method switches on the same enum. The state logic is duplicated
 * and the transition rules are smeared across methods, so it is easy to
 * create an inconsistent transition. Adding a state means editing every
 * method's switch.
 */
public class Document {

    enum State { DRAFT, MODERATION, PUBLISHED }

    private State state = State.DRAFT;

    public void publish() {
        switch (state) {
            case DRAFT:
                state = State.MODERATION;
                System.out.println("Sent to moderation");
                break;
            case MODERATION:
                state = State.PUBLISHED;
                System.out.println("Published");
                break;
            case PUBLISHED:
                System.out.println("Already published");
                break;
        }
    }

    public static void main(String[] args) {
        Document doc = new Document();
        doc.publish(); // -> moderation
        doc.publish(); // -> published
        doc.publish(); // already published
    }
}
