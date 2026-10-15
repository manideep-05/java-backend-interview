package without;

import java.util.ArrayList;
import java.util.List;

/**
 * FLYWEIGHT NOT USED — the memory problem.
 *
 * Every Tree stores its OWN copy of the heavy, identical data (name,
 * color, texture). Planting 1,000,000 oaks means a million duplicate
 * copies of the same texture bytes — wasteful.
 */
public class Main {

    static class Tree {
        int x, y;                 // extrinsic (unique per tree)
        String name;              // intrinsic (same for all oaks)
        String color;             // intrinsic
        byte[] texture;           // intrinsic — the heavy part, duplicated!

        Tree(int x, int y, String name, String color, byte[] texture) {
            this.x = x;
            this.y = y;
            this.name = name;
            this.color = color;
            this.texture = texture;
        }
    }

    public static void main(String[] args) {
        List<Tree> forest = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            // Each tree allocates its own 1 KB texture copy.
            forest.add(new Tree(i, i, "Oak", "Green", new byte[1024]));
        }
        System.out.println("Planted " + forest.size() + " trees, each with its own texture copy.");
    }
}
