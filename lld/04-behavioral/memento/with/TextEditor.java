package with;

/**
 * MEMENTO APPLIED — the originator.
 *
 * It owns its state (private) and is the ONLY one that can create or
 * read a Memento. External code gets an opaque snapshot it cannot peek
 * into, so encapsulation is preserved.
 */
public class TextEditor {

    private String content = "";

    public void type(String text) {
        content += text;
    }

    public String getContent() {
        return content;
    }

    /** Produce an opaque snapshot of the current state. */
    public Memento save() {
        return new Memento(content);
    }

    /** Restore state from a previously saved snapshot. */
    public void restore(Memento memento) {
        this.content = memento.getState();
    }

    /**
     * The memento. Its state accessor is package-private so only the
     * originator (same package) can read it — the caretaker cannot.
     */
    public static class Memento {
        private final String state;

        private Memento(String state) {
            this.state = state;
        }

        String getState() { // package-private: hidden from outside callers
            return state;
        }
    }
}
