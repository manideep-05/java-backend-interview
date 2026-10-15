package with;

/**
 * LSP APPLIED.
 *
 * Rectangle is an independent Shape. It has two independent dimensions
 * set once at construction, so there are no misleading setters and no
 * contract for a subclass to violate. (Immutability also helps.)
 */
public class Rectangle implements Shape {
    private final int width;
    private final int height;

    public Rectangle(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public int getArea() {
        return width * height;
    }
}
