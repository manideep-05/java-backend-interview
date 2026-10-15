package with;

/**
 * The element interface. accept() is the key: an element calls back the
 * visitor's matching visit() method ("double dispatch"), so the right
 * overload runs based on the concrete element type.
 */
public interface Shape {
    void accept(ShapeVisitor visitor);
}
