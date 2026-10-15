package with;

/**
 * A concrete command. It holds its receiver and knows both how to
 * execute and how to undo itself.
 */
public class LightOnCommand implements Command {

    private final Light light;

    public LightOnCommand(Light light) {
        this.light = light;
    }

    @Override
    public void execute() {
        light.on();
    }

    @Override
    public void undo() {
        light.off(); // the inverse of execute()
    }
}
