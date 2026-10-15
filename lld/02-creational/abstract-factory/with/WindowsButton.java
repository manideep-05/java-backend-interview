package with;

/** Concrete product — Windows family. */
public class WindowsButton implements Button {
    @Override
    public void render() {
        System.out.println("[ Windows Button ]");
    }
}
