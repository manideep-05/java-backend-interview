package production;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * PRODUCTION document (leaf + Memento).
 *
 * Concurrency design:
 *  - edit/save/restore mutate content AND the version history together,
 *    so each is `synchronized` on this document. Per-document locking:
 *    different documents never block each other; concurrent edits to the
 *    SAME doc are serialized so the version list can't be corrupted.
 *  - history is a CopyOnWriteArrayList so a reader (versionCount/getSize)
 *    never sees a torn list while a writer appends under the lock.
 */
public class Document implements Node {

    public static final class Memento {
        private final String content;
        private Memento(String content) { this.content = content; }
    }

    private final String name;
    private volatile String content = "";
    private final List<Memento> history = new CopyOnWriteArrayList<>();

    public Document(String name) {
        this.name = name;
    }

    public synchronized void edit(String newContent) {
        this.content = newContent;
    }

    public synchronized void save() {
        history.add(new Memento(content));
    }

    public synchronized void restore(int version) {
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
