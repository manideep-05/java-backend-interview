package with;

/**
 * VISITOR APPLIED — the visitor interface.
 *
 * One visit method per concrete element type. A single operation
 * (area, export, ...) is implemented as one class that visits all
 * element types.
 */
public interface ShapeVisitor {
    void visit(Circle circle);
    void visit(Rectangle rectangle);
}
