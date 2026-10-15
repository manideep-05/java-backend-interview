package without;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * MEMENTO NOT USED — the problem.
 *
 * To support undo, the caretaker (main) reaches directly into the
 * editor's state and manually copies the raw String out and back. The
 * editor's internals are exposed, and the undo logic is coupled to how
 * state happens to be stored today.
 */
public class Main {

    static class TextEditor {
        String content = ""; // exposed so external code can snapshot it
    }

    public static void main(String[] args) {
        TextEditor editor = new TextEditor();
        Deque<String> history = new ArrayDeque<>();

        editor.content = "Hello";
        history.push(editor.content); // caretaker touches internal field

        editor.content = "Hello World";
        history.push(editor.content);

        editor.content = "Hello World!!!";

        // Undo: caretaker writes directly back into internal state.
        editor.content = history.pop();
        System.out.println("After undo: " + editor.content); // Hello World
    }
}
