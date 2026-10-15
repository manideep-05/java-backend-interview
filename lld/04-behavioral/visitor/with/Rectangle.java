package with;

/** Another concrete element — data only. */
public class Rectangle implements Shape {

    public final double width;
    public final double height;

    public Rectangle(double width, double height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public void accept(ShapeVisitor visitor) {
        visitor.visit(this); // picks visit(Rectangle)
    }
}
