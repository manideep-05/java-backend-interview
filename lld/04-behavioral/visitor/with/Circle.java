package with;

/**
 * A concrete element. It carries only its own DATA — no operations.
 * accept() dispatches to the visitor's visit(Circle) overload.
 */
public class Circle implements Shape {

    public final double radius;

    public Circle(double radius) {
        this.radius = radius;
    }

    @Override
    public void accept(ShapeVisitor visitor) {
        visitor.visit(this); // picks visit(Circle)
    }
}
