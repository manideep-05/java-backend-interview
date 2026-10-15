package normal;

/**
 * FACADE offering a path-based API over the root directory. Path
 * resolution walks the tree segment by segment.
 */
public class FileSystem {
    private final Directory root = new Directory("/");

    /** Resolve a parent directory from an absolute path, creating nothing. */
    private Directory resolveParent(String[] segments) {
        Directory current = root;
        for (int i = 0; i < segments.length - 1; i++) {
            FileSystemNode next = current.getChild(segments[i]);
            if (!(next instanceof Directory)) {
                throw new IllegalArgumentException("Not a directory: " + segments[i]);
            }
            current = (Directory) next;
        }
        return current;
    }

    public void mkdir(String path) {
        String[] seg = split(path);
        resolveParent(seg).add(new Directory(seg[seg.length - 1]));
    }

    public void touch(String path, int size) {
        String[] seg = split(path);
        resolveParent(seg).add(new File(seg[seg.length - 1], size));
    }

    public int size(String path) {
        String[] seg = split(path);
        FileSystemNode node = resolveParent(seg).getChild(seg[seg.length - 1]);
        if (node == null) {
            throw new IllegalArgumentException("Not found: " + path);
        }
        return node.getSize();
    }

    private String[] split(String path) {
        return path.replaceAll("^/", "").split("/");
    }

    public static void main(String[] args) {
        FileSystem fs = new FileSystem();
        fs.mkdir("/a");
        fs.mkdir("/a/b");
        fs.touch("/a/b/c.txt", 100);
        fs.touch("/a/note.txt", 50);
        System.out.println("Size of /a: " + fs.size("/a"));         // 150
        System.out.println("Size of /a/b/c.txt: " + fs.size("/a/b/c.txt")); // 100
    }
}
