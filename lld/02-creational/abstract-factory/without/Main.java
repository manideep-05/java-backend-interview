package without;

/**
 * ABSTRACT FACTORY NOT USED — the problem.
 *
 * The client hand-picks concrete widgets. Nothing stops it from mixing
 * a Windows button with a Mac checkbox, producing an inconsistent UI.
 * Adding a new OS means editing every place that constructs widgets.
 */
public class Main {

    // Stand-in concrete widgets (normally separate classes).
    static class WindowsButton { void render() { System.out.println("[Windows Button]"); } }
    static class MacCheckbox   { void render() { System.out.println("( Mac Checkbox )"); } }

    public static void main(String[] args) {
        // Oops: a Windows button paired with a Mac checkbox. Inconsistent!
        WindowsButton button = new WindowsButton();
        MacCheckbox checkbox = new MacCheckbox();

        button.render();
        checkbox.render();
    }
}
