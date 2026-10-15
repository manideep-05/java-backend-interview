package production;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PRODUCTION inventory.
 *
 * Stock per product is an AtomicInteger. reserve() uses a CAS loop so
 * two concurrent checkouts for the last unit can't both succeed — the
 * decrement only commits if the quantity hasn't changed under us. This
 * is the anti-oversell guarantee.
 */
public class Inventory {
    private final Map<String, AtomicInteger> stock = new ConcurrentHashMap<>();

    public void set(String product, int qty) {
        stock.put(product, new AtomicInteger(qty));
    }

    public boolean reserve(String product, int qty) {
        AtomicInteger s = stock.get(product);
        if (s == null) {
            return false;
        }
        while (true) {
            int cur = s.get();
            if (cur < qty) {
                return false; // not enough stock
            }
            if (s.compareAndSet(cur, cur - qty)) {
                return true;  // we atomically claimed the units
            }
            // lost the race; retry with fresh value
        }
    }

    public void release(String product, int qty) {
        stock.computeIfAbsent(product, k -> new AtomicInteger(0)).addAndGet(qty);
    }

    public int available(String product) {
        AtomicInteger s = stock.get(product);
        return s == null ? 0 : s.get();
    }
}
