package production;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * PRODUCTION facade.
 *
 * Path resolution is read-only over ConcurrentHashMaps (safe). Creation
 * uses Directory.addIfAbsent (atomic putIfAbsent), so concurrent creates
 * of the same path can't duplicate a node. No global lock — directories
 * are independent and each map is internally concurrent.
 */
public class FileSystem {
    private final Directory root = new Directory("/");

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

    /** Create a directory if absent; returns the directory now at that path. */
    public Directory mkdir(String path) {
        String[] seg = split(path);
        Directory parent = resolveParent(seg);
        FileSystemNode node = parent.addIfAbsent(new Directory(seg[seg.length - 1]));
        return (Directory) node;
    }

    public void touch(String path, int size) {
        String[] seg = split(path);
        resolveParent(seg).addIfAbsent(new File(seg[seg.length - 1], size));
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

    public static void main(String[] args) throws InterruptedException {
        FileSystem fs = new FileSystem();
        fs.mkdir("/a");

        // Many threads race to create the SAME directory /a/shared.
        AtomicInteger created = new AtomicInteger();
        Runnable r = () -> {
            Directory before = fs.mkdir("/a"); // ensure /a exists
            // addIfAbsent on /a: only the first create of "shared" is new.
            FileSystemNode node = before.addIfAbsent(new Directory("shared"));
            if (node.getName().equals("shared")) {
                created.incrementAndGet(); // counts references, but only ONE object exists
            }
        };
        Thread[] ts = new Thread[8];
        for (int i = 0; i < ts.length; i++) {
            ts[i] = new Thread(r);
            ts[i].start();
        }
        for (Thread t : ts) {
            t.join();
        }

        fs.touch("/a/shared/x.txt", 42);
        System.out.println("Size of /a/shared (single dir, no duplicates): " + fs.size("/a/shared"));
    }
}
