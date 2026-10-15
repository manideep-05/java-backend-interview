package production;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe inventory.
 *
 * counts use AtomicInteger so decrement is atomic. decrementIfAvailable
 * uses a CAS loop so we never dispense a unit that isn't there (no
 * overselling the last item when two machines-threads race).
 */
public class Inventory {
    private final Map<String, Product> products = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> counts = new ConcurrentHashMap<>();

    public void add(Product product, int count) {
        products.put(product.getCode(), product);
        counts.computeIfAbsent(product.getCode(), k -> new AtomicInteger(0)).addAndGet(count);
    }

    public boolean isAvailable(String code) {
        AtomicInteger c = counts.get(code);
        return c != null && c.get() > 0;
    }

    public Product get(String code) {
        return products.get(code);
    }

    /** Atomically take one unit if available. Returns false if sold out. */
    public boolean decrementIfAvailable(String code) {
        AtomicInteger c = counts.get(code);
        if (c == null) {
            return false;
        }
        while (true) {
            int cur = c.get();
            if (cur <= 0) {
                return false;
            }
            if (c.compareAndSet(cur, cur - 1)) {
                return true; // this thread took the unit
            }
            // else: lost the race, retry with the fresh value
        }
    }
}
