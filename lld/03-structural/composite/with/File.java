package with;

/**
 * The LEAF. It has no children; getSize() just returns its own size.
 */
public class File implements FileSystemNode {

    private final String name;
    private final int size;

    public File(String name, int size) {
        this.name = name;
        this.size = size;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getSize() {
        return size;
    }
}
