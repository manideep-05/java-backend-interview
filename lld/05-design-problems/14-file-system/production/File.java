package production;

/** Immutable leaf (safe to share across threads). */
public final class File implements FileSystemNode {
    private final String name;
    private final int size;

    public File(String name, int size) {
        this.name = name;
        this.size = size;
    }

    @Override public String getName() { return name; }
    @Override public int getSize() { return size; }
}
