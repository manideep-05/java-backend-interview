package without;

import java.util.ArrayList;
import java.util.List;

/**
 * COMPOSITE NOT USED — the problem.
 *
 * Files and folders are different types with no shared abstraction, so
 * the client must type-check (instanceof) and manually recurse to total
 * up sizes. This branching logic spreads wherever the tree is walked.
 */
public class Main {

    static class File {
        String name; int size;
        File(String name, int size) { this.name = name; this.size = size; }
    }

    static class Folder {
        String name;
        List<Object> children = new ArrayList<>(); // Files or Folders, untyped
        Folder(String name) { this.name = name; }
    }

    // Client has to know the concrete types and recurse by hand.
    static int totalSize(Object node) {
        if (node instanceof File) {
            return ((File) node).size;
        } else if (node instanceof Folder) {
            int sum = 0;
            for (Object child : ((Folder) node).children) {
                sum += totalSize(child); // manual recursion + branching
            }
            return sum;
        }
        throw new IllegalArgumentException("Unknown node");
    }

    public static void main(String[] args) {
        Folder root = new Folder("root");
        root.children.add(new File("a.txt", 100));
        Folder sub = new Folder("sub");
        sub.children.add(new File("b.txt", 200));
        root.children.add(sub);

        System.out.println("Total size = " + totalSize(root)); // 300
    }
}
