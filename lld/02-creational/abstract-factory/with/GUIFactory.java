package with;

/**
 * ABSTRACT FACTORY APPLIED — the abstract factory.
 *
 * It declares a creation method for EACH product in the family. A given
 * concrete factory returns products from ONE consistent family only.
 */
public interface GUIFactory {
    Button createButton();
    Checkbox createCheckbox();
}
