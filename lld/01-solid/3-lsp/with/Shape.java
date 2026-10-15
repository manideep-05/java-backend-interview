package with;

/**
 * LSP APPLIED — a common abstraction that every shape can honestly fulfill.
 *
 * The contract is minimal and true for all shapes: "I can report my area."
 * No shape is forced to pretend it behaves like another.
 */
public interface Shape {
    int getArea();
}
