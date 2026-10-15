package without;

/**
 * LSP VIOLATION.
 *
 * A Square keeps all sides equal, so it must override the setters to
 * change BOTH dimensions. But that BREAKS the Rectangle contract:
 * callers expect setWidth to leave height unchanged. A Square cannot
 * honor that promise, so it is not a safe substitute for a Rectangle.
 */
public class Square extends Rectangle {

    @Override
    public void setWidth(int width) {
        this.width = width;
        this.height = width; // side effect the parent contract forbids
    }

    @Override
    public void setHeight(int height) {
        this.width = height; // side effect the parent contract forbids
        this.height = height;
    }
}
