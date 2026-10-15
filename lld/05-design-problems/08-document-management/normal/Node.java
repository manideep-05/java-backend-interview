package normal;

/**
 * COMPOSITE component: both Document (leaf) and Folder (composite)
 * implement this, so clients treat a single doc and a whole subtree
 * uniformly (no instanceof branching).
 */
public interface Node {
    String getName();
    int getSize();
}
