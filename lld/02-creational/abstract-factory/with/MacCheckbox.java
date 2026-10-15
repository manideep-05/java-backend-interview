package with;

/** Concrete product — Mac family. */
public class MacCheckbox implements Checkbox {
    @Override
    public void render() {
        System.out.println("(o) Mac Checkbox");
    }
}
