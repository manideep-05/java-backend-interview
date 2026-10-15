package normal;

/**
 * Stock for one SKU: quantity + reorder threshold. `alerted` tracks
 * whether a low-stock alert is already outstanding, so we fire once per
 * crossing (not on every deduction) — avoiding alert storms.
 */
public class StockItem {
    private int quantity;
    private final int reorderThreshold;
    private boolean alerted = false;

    public StockItem(int quantity, int reorderThreshold) {
        this.quantity = quantity;
        this.reorderThreshold = reorderThreshold;
    }

    public boolean reserve(int qty) {
        if (quantity >= qty) {       // check-then-subtract (normal)
            quantity -= qty;
            return true;
        }
        return false;
    }

    public void restock(int qty) {
        quantity += qty;
        if (quantity >= reorderThreshold) {
            alerted = false; // crossing back above clears the alert latch
        }
    }

    /** Returns true exactly on the crossing below threshold. */
    public boolean crossedThreshold() {
        if (quantity < reorderThreshold && !alerted) {
            alerted = true;
            return true;
        }
        return false;
    }

    public int getQuantity() { return quantity; }
}
