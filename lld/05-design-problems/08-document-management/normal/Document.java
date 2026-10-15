package normal;

import java.util.ArrayList;
import java.util.List;

/**
 * LEAF node + MEMENTO originator/caretaker.
 *
 * The document owns its content and a history of version snapshots.
 * save() pushes an opaque Memento; restore(v) sets content from one.
 * The Memento's content is only readable by Document (inner class), so
 * encapsulation is preserved.
 */
public class Document implements Node {

    /** Opaque version snapshot. */
    public static final class Memento {
        private final String content;
        private Memento(String content) { this.content = content; }
    }

    private final String name;
    private String content = "";
    private final List<Memento> history = new ArrayList<>();

    public Document(String name) {
        this.name = name;
    }

    public void edit(String newContent) {
        this.content = newContent;
    }

    /** Snapshot current content as a new version. */
    public void save() {
        history.add(new Memento(content));
    }

    /** Restore content from a saved version index. */
    public void restore(int version) {
        if (version < 0 || version >= history.size()) {
            throw new IllegalArgumentException("No such version: " + version);
        }
        this.content = history.get(version).content;
    }

    public String getContent() { return content; }
    public int versionCount() { return history.size(); }

    @Override public String getName() { return name; }
    @Override public int getSize() { return content.length(); }
}
