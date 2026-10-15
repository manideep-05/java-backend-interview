package with;

/** Concrete product — Windows family. */
public class WindowsCheckbox implements Checkbox {
    @Override
    public void render() {
        System.out.println("[x] Windows Checkbox");
    }
}
