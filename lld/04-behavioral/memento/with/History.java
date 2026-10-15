package with;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * The caretaker.
 *
 * It stores mementos (the undo stack) but treats each as a black box —
 * it never reads or modifies the state inside. It only pushes and pops.
 */
public class History {

    private final Deque<TextEditor.Memento> stack = new ArrayDeque<>();

    public void push(TextEditor.Memento memento) {
        stack.push(memento);
    }

    public TextEditor.Memento pop() {
        return stack.pop();
    }

    public boolean isEmpty() {
        return stack.isEmpty();
    }
}
