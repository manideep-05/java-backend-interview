package normal;

/**
 * COMPOSITE component: File (leaf) and Directory (composite) implement
 * this, so clients treat a file and a whole directory tree uniformly.
 */
public interface FileSystemNode {
    String getName();
    int getSize();
}
