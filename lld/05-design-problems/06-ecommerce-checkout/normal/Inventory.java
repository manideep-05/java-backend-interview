package normal;

import java.util.HashMap;
import java.util.Map;

/**
 * Stock bookkeeping. NOTE (normal): reserve() is check-then-subtract,
 * fine single-threaded; production makes it atomic to prevent oversell.
 */
public class Inventory {
    private final Map<String, Integer> stock = new HashMap<>();

    public void set(String product, int qty) {
        stock.put(product, qty);
    }

    public boolean reserve(String product, int qty) {
        int available = stock.getOrDefault(product, 0);
        if (available >= qty) {
            stock.put(product, available - qty);
            return true;
        }
        return false;
    }

    public void release(String product, int qty) {
        stock.merge(product, qty, Integer::sum);
    }
}
