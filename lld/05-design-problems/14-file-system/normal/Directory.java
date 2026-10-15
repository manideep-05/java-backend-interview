package normal;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * COMPOSITE: children keyed by name. getSize recurses over children
 * (the recursion lives here, not in the client). Also resolves a single
 * child by name for path walking.
 */
public class Directory implements FileSystemNode {
    private final String name;
    private final Map<String, FileSystemNode> children = new LinkedHashMap<>();

    public Directory(String name) {
        this.name = name;
    }

    public void add(FileSystemNode node) {
        children.put(node.getName(), node);
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
            total += child.getSize(); // file or sub-directory, uniform
        }
        return total;
    }
}
