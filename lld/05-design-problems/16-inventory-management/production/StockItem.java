package production;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PRODUCTION stock item.
 *
 * quantity is an AtomicInteger; reserve() uses a CAS loop that only
 * commits `cur - qty` when `cur >= qty` AND nobody changed `cur` in
 * between. This is the anti-oversell / no-negative guarantee under
 * concurrent orders.
 *
 * `alerted` is an AtomicBoolean latch so the low-stock event fires once
 * per crossing even if many threads deduct simultaneously.
 */
public class StockItem {
    private final AtomicInteger quantity;
    private final int reorderThreshold;
    private final AtomicBoolean alerted = new AtomicBoolean(false);

    public StockItem(int quantity, int reorderThreshold) {
        this.quantity = new AtomicInteger(quantity);
        this.reorderThreshold = reorderThreshold;
    }

    public boolean reserve(int qty) {
        while (true) {
            int cur = quantity.get();
            if (cur < qty) {
                return false; // not enough; never go negative
            }
            if (quantity.compareAndSet(cur, cur - qty)) {
                return true;  // atomically deducted
            }
            // lost the race; retry with fresh value
        }
    }

    public void restock(int qty) {
        int now = quantity.addAndGet(qty);
        if (now >= reorderThreshold) {
            alerted.set(false); // crossing back up clears the latch
        }
    }

    /** True exactly once per crossing: CAS the latch false->true. */
    public boolean crossedThreshold() {
        return quantity.get() < reorderThreshold && alerted.compareAndSet(false, true);
    }

    public int getQuantity() {
        return quantity.get();
    }
}
