package with;

/**
 * The client calls getSize() on a node without caring whether it's a
 * single file or an entire directory tree. No instanceof, no manual
 * recursion.
 */
public class Main {
    public static void main(String[] args) {
        Folder root = new Folder("root")
                .add(new File("a.txt", 100))
                .add(new Folder("sub")
                        .add(new File("b.txt", 200)));

        FileSystemNode node = root; // treated uniformly as a node
        System.out.println("Total size = " + node.getSize()); // 300
    }
}
