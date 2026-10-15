package with;

/**
 * The shared interface. Both the real object and the proxy implement it,
 * so the client can't tell which one it's holding.
 */
public interface Image {
    void display();
}
