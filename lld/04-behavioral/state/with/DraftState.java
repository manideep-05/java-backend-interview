package with;

/** Concrete state: a draft moves to moderation when published. */
public class DraftState implements DocumentState {
    @Override
    public void publish(Document context) {
        System.out.println("Sent to moderation");
        context.setState(new ModerationState()); // the state drives the transition
    }

    @Override
    public String name() {
        return "Draft";
    }
}
