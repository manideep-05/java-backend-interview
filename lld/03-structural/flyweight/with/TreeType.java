package with;

/**
 * FLYWEIGHT APPLIED — the flyweight itself.
 *
 * Holds only the INTRINSIC (shared, context-free) state: name, color,
 * and the heavy texture. One instance is shared by every tree of this
 * type, so the texture is stored once, not per tree.
 */
public class TreeType {

    private final String name;
    private final String color;
    private final byte[] texture; // heavy, shared

    public TreeType(String name, String color, byte[] texture) {
        this.name = name;
        this.color = color;
        this.texture = texture;
    }

    // Extrinsic state (x, y) is PASSED IN, never stored here.
    public void draw(int x, int y) {
        System.out.println("Drawing " + color + " " + name + " at (" + x + ", " + y + ")");
    }
}
