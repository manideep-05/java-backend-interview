package with;

/**
 * STATE APPLIED — the context.
 *
 * It holds a current DocumentState and delegates publish() to it. It
 * contains no switch: each state owns its own behavior and transition.
 */
public class Document {

    private DocumentState state = new DraftState(); // initial state

    void setState(DocumentState state) {
        this.state = state;
    }

    public void publish() {
        state.publish(this); // behavior depends on current state
    }

    public String currentState() {
        return state.name();
    }

    public static void main(String[] args) {
        Document doc = new Document();
        doc.publish(); // Draft -> Moderation
        doc.publish(); // Moderation -> Published
        doc.publish(); // Published (stays)
    }
}
