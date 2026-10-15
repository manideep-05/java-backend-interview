package normal;

/** LEAF: a file with a name and a size (e.g. content length). */
public class File implements FileSystemNode {
    private final String name;
    private final int size;

    public File(String name, int size) {
        this.name = name;
        this.size = size;
    }

    @Override public String getName() { return name; }
    @Override public int getSize() { return size; }
}
