package with;

/**
 * The client receives ONE factory and builds its UI from it. It cannot
 * mix families: whatever factory is injected, button and checkbox are
 * guaranteed to match.
 */
public class Application {

    private final Button button;
    private final Checkbox checkbox;

    public Application(GUIFactory factory) {
        this.button = factory.createButton();
        this.checkbox = factory.createCheckbox();
    }

    public void renderUI() {
        button.render();
        checkbox.render();
    }
}
