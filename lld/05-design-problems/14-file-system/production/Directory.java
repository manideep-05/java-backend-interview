package production;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PRODUCTION composite directory.
 *
 * Concurrency design:
 *  - children is a ConcurrentHashMap, so lookup/iterate/remove on a
 *    directory are thread-safe without a directory-wide lock.
 *  - addIfAbsent uses putIfAbsent so "create a child with this name"
 *    is ATOMIC: two threads racing to create /a/x — exactly one wins,
 *    the other sees the existing node. No duplicate-create race.
 *  - Different directories never contend (per-directory map).
 */
public class Directory implements FileSystemNode {
    private final String name;
    private final Map<String, FileSystemNode> children = new ConcurrentHashMap<>();

    public Directory(String name) {
        this.name = name;
    }

    /** Returns the node now present under childName (existing or newly added). */
    public FileSystemNode addIfAbsent(FileSystemNode node) {
        FileSystemNode prior = children.putIfAbsent(node.getName(), node);
        return prior != null ? prior : node;
    }

    public FileSystemNode getChild(String childName) {
        return children.get(childName);
    }

    public void remove(String childName) {
        children.remove(childName);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getSize() {
        int total = 0;
        for (FileSystemNode child : children.values()) {
            total += child.getSize();
        }
        return total;
    }
}
