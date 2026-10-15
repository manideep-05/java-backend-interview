package with;

/**
 * The state interface. Each concrete state implements how publish()
 * behaves AND which state comes next.
 */
public interface DocumentState {
    void publish(Document context);
    String name();
}
