package with;

/**
 * COMPOSITE APPLIED — the common component.
 *
 * Both leaves (File) and composites (Folder) implement this. Because
 * they share one type, the client treats a single file and a whole
 * tree identically.
 */
public interface FileSystemNode {
    String getName();
    int getSize();
}
