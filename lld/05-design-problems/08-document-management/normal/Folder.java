package normal;

import java.util.ArrayList;
import java.util.List;

/**
 * COMPOSITE node: holds child Nodes (documents or folders) and computes
 * aggregate operations by recursing into children. The recursion is
 * encapsulated here, not in the client.
 */
public class Folder implements Node {
    private final String name;
    private final List<Node> children = new ArrayList<>();

    public Folder(String name) {
        this.name = name;
    }

    public Folder add(Node node) {
        if (node == this) {
            throw new IllegalArgumentException("A folder cannot contain itself");
        }
        children.add(node);
        return this;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getSize() {
        int total = 0;
        for (Node child : children) {
            total += child.getSize(); // child may be a Document or a Folder
        }
        return total;
    }

    public static void main(String[] args) {
        Document readme = new Document("README");
        readme.edit("hello");
        readme.save();              // version 0: "hello"
        readme.edit("hello world");
        readme.save();              // version 1: "hello world"
        readme.restore(0);          // back to "hello"

        Folder root = new Folder("root").add(readme).add(new Folder("sub").add(new Document("note")));
        System.out.println("README content: " + readme.getContent());
        System.out.println("Tree size: " + root.getSize());
    }
}
