package with;

import java.util.HashMap;
import java.util.Map;

/**
 * The flyweight factory.
 *
 * Caches TreeType instances by their intrinsic key so that asking for
 * "Oak/Green" twice returns the SAME shared object. This is what caps
 * memory: one TreeType per unique combination, not per tree.
 */
public class TreeTypeFactory {

    private static final Map<String, TreeType> cache = new HashMap<>();

    public static TreeType getTreeType(String name, String color, byte[] texture) {
        String key = name + "-" + color;
        // computeIfAbsent creates it only the first time.
        return cache.computeIfAbsent(key, k -> {
            System.out.println("Creating shared TreeType: " + key);
            return new TreeType(name, color, texture);
        });
    }

    public static int distinctTypes() {
        return cache.size();
    }
}
