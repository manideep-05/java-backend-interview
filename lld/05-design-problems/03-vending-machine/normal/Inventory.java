package normal;

import java.util.HashMap;
import java.util.Map;

/**
 * Inventory: product code -> (product, remaining count).
 * Single responsibility: stock bookkeeping. The machine asks it whether
 * a product is available and tells it to decrement on dispense.
 */
public class Inventory {
    private final Map<String, Product> products = new HashMap<>();
    private final Map<String, Integer> counts = new HashMap<>();

    public void add(Product product, int count) {
        products.put(product.getCode(), product);
        counts.merge(product.getCode(), count, Integer::sum);
    }

    public boolean isAvailable(String code) {
        return counts.getOrDefault(code, 0) > 0;
    }

    public Product get(String code) {
        return products.get(code);
    }

    public void decrement(String code) {
        counts.computeIfPresent(code, (k, v) -> v - 1);
    }
}
