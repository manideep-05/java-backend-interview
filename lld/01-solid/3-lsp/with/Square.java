package with;

/**
 * LSP APPLIED.
 *
 * Square is NOT a Rectangle — it is its own Shape with a single side.
 * Because it never claims to be a Rectangle, it can never violate the
 * Rectangle contract. Each type is substitutable for Shape honestly.
 */
public class Square implements Shape {
    private final int side;

    public Square(int side) {
        this.side = side;
    }

    @Override
    public int getArea() {
        return side * side;
    }
}
