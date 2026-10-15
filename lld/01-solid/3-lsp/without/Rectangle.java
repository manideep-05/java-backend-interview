package without;

/**
 * The base class. Its implicit CONTRACT is:
 *   - setWidth(w) changes only the width; height is untouched.
 *   - setHeight(h) changes only the height; width is untouched.
 * Callers are allowed to rely on this.
 */
public class Rectangle {
    protected int width;
    protected int height;

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getArea() {
        return width * height;
    }
}
