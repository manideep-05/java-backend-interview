package with;

/**
 * Choose the family once (e.g. based on the OS at startup). The rest of
 * the app is oblivious to concrete widget classes.
 */
public class Main {
    public static void main(String[] args) {
        String os = System.getProperty("os.name").toLowerCase();

        GUIFactory factory = os.contains("mac") ? new MacFactory() : new WindowsFactory();

        Application app = new Application(factory);
        app.renderUI(); // a consistent family, guaranteed
    }
}
