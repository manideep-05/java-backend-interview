package with;

/** Terminal state: publishing again does nothing. */
public class PublishedState implements DocumentState {
    @Override
    public void publish(Document context) {
        System.out.println("Already published");
    }

    @Override
    public String name() {
        return "Published";
    }
}
