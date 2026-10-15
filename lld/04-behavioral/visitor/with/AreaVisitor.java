package with;

/**
 * One operation = one visitor. This visitor computes areas. All the
 * area logic lives in ONE place instead of being spread across shapes.
 */
public class AreaVisitor implements ShapeVisitor {

    private double totalArea = 0;

    @Override
    public void visit(Circle circle) {
        totalArea += Math.PI * circle.radius * circle.radius;
    }

    @Override
    public void visit(Rectangle rectangle) {
        totalArea += rectangle.width * rectangle.height;
    }

    public double getTotalArea() {
        return totalArea;
    }
}
