package with;

/**
 * The editor snapshots itself before each change; the history stores
 * the opaque mementos. Undo asks the editor to restore from the last
 * snapshot. No external code ever touches the editor's internals.
 */
public class Main {
    public static void main(String[] args) {
        TextEditor editor = new TextEditor();
        History history = new History();

        editor.type("Hello");
        history.push(editor.save());  // snapshot "Hello"

        editor.type(" World");
        history.push(editor.save());  // snapshot "Hello World"

        editor.type("!!!");
        System.out.println("Current: " + editor.getContent()); // Hello World!!!

        editor.restore(history.pop()); // undo -> "Hello World"
        System.out.println("After undo: " + editor.getContent());

        editor.restore(history.pop()); // undo -> "Hello"
        System.out.println("After undo: " + editor.getContent());
    }
}
