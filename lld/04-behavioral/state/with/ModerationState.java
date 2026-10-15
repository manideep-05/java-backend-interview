package with;

/** Concrete state: moderation moves to published. */
public class ModerationState implements DocumentState {
    @Override
    public void publish(Document context) {
        System.out.println("Published");
        context.setState(new PublishedState());
    }

    @Override
    public String name() {
        return "Moderation";
    }
}
