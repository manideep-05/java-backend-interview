package with;

/**
 * The context object.
 *
 * Stores only the EXTRINSIC state (its position) plus a REFERENCE to a
 * shared TreeType. Thousands of trees of the same type share one
 * TreeType, so the heavy texture exists once in memory.
 */
public class Tree {

    private final int x;
    private final int y;
    private final TreeType type; // shared flyweight reference

    public Tree(int x, int y, TreeType type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }

    public void draw() {
        type.draw(x, y); // pass extrinsic state into the shared flyweight
    }
}
