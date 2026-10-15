package with;

/**
 * The command interface. execute() does the work; undo() reverses it,
 * which is what makes undo/redo possible.
 */
public interface Command {
    void execute();
    void undo();
}
