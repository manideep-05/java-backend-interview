package without;

/**
 * COMMAND NOT USED — the problem.
 *
 * The remote is hard-wired to specific devices via a string switch.
 * It cannot support undo, cannot queue actions, and every new device
 * or action means editing this method.
 */
public class RemoteControl {

    static class Light { void on() { System.out.println("Light ON"); } void off() { System.out.println("Light OFF"); } }

    private final Light light = new Light();

    public void press(String button) {
        if (button.equals("light_on")) {
            light.on();
        } else if (button.equals("light_off")) {
            light.off();
        } else {
            throw new IllegalArgumentException("Unknown button");
        }
        // No way to undo the last action.
    }

    public static void main(String[] args) {
        RemoteControl r = new RemoteControl();
        r.press("light_on");
        r.press("light_off");
    }
}
