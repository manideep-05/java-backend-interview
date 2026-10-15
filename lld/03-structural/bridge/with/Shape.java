package with;

/**
 * BRIDGE APPLIED — the ABSTRACTION hierarchy (one dimension: shape).
 *
 * It HOLDS a reference to a Color implementation (the "bridge"). Shape
 * and Color now vary INDEPENDENTLY: adding a color needs no shape
 * change, and adding a shape needs no color change. N x M becomes N + M.
 */
public abstract class Shape {

    protected final Color color; // the bridge to the implementation side

    protected Shape(Color color) {
        this.color = color;
    }

    public abstract void draw();
}
