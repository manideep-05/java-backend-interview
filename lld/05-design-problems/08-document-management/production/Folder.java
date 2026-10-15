package production;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * PRODUCTION composite. children is a CopyOnWriteArrayList so size/listing
 * reads iterate lock-free while the (rare) structural changes are safe.
 */
public class Folder implements Node {
    private final String name;
    private final List<Node> children = new CopyOnWriteArrayList<>();

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
            total += child.getSize();
        }
        return total;
    }

    public static void main(String[] args) throws InterruptedException {
        Document doc = new Document("shared");

        // Two editors save versions concurrently; history stays consistent.
        Runnable editor = () -> {
            for (int i = 0; i < 50; i++) {
                doc.edit(Thread.currentThread().getName() + "-" + i);
                doc.save();
            }
        };
        Thread t1 = new Thread(editor, "A"), t2 = new Thread(editor, "B");
        t1.start(); t2.start(); t1.join(); t2.join();

        Folder root = new Folder("root").add(doc);
        System.out.println("Versions saved (must be 100): " + doc.versionCount()
                + ", tree size: " + root.getSize());
    }
}
