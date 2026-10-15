package with;

import java.util.ArrayList;
import java.util.List;

/**
 * Five trees, but only ONE shared TreeType is ever created. The heavy
 * texture lives once; each Tree is just coordinates + a reference.
 */
public class Main {
    public static void main(String[] args) {
        List<Tree> forest = new ArrayList<>();
        byte[] oakTexture = new byte[1024]; // loaded once

        for (int i = 0; i < 5; i++) {
            TreeType oak = TreeTypeFactory.getTreeType("Oak", "Green", oakTexture);
            forest.add(new Tree(i, i, oak)); // all share the same TreeType
        }

        forest.forEach(Tree::draw);
        System.out.println("Trees planted: " + forest.size()
                + ", distinct TreeTypes in memory: " + TreeTypeFactory.distinctTypes());
        // Trees planted: 5, distinct TreeTypes in memory: 1
    }
}
