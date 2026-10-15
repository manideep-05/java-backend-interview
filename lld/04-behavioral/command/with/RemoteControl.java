package with;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * COMMAND APPLIED — the invoker.
 *
 * It knows nothing about lights. It just executes Commands and keeps a
 * history stack so it can undo the last one. Any new action is a new
 * Command class — no change here.
 */
public class RemoteControl {

    private final Deque<Command> history = new ArrayDeque<>();

    public void submit(Command command) {
        command.execute();
        history.push(command); // remember for undo
    }

    public void undoLast() {
        if (!history.isEmpty()) {
            Command last = history.pop();
            System.out.print("[undo] ");
            last.undo();
        }
    }

    public static void main(String[] args) {
        Light light = new Light();
        RemoteControl remote = new RemoteControl();

        remote.submit(new LightOnCommand(light));  // Light ON
        remote.submit(new LightOffCommand(light)); // Light OFF
        remote.undoLast();                         // [undo] Light ON
        remote.undoLast();                         // [undo] Light OFF
    }
}
