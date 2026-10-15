package with;

import java.util.ArrayList;
import java.util.List;

/**
 * The COMPOSITE. It holds children (which are themselves FileSystemNodes
 * — files or folders) and implements getSize() by delegating to each
 * child. The recursion is encapsulated here, not in the client.
 */
public class Folder implements FileSystemNode {

    private final String name;
    private final List<FileSystemNode> children = new ArrayList<>();

    public Folder(String name) {
        this.name = name;
    }

    public Folder add(FileSystemNode node) {
        children.add(node);
        return this;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getSize() {
        int sum = 0;
        for (FileSystemNode child : children) {
            sum += child.getSize(); // uniform call — child may be a File or a Folder
        }
        return sum;
    }
}
