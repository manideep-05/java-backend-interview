package normal;

/**
 * OBSERVER: notified when a SKU crosses below its reorder threshold.
 * The Inventory depends only on this interface, not on concrete
 * procurement/dashboard systems (DIP).
 */
public interface InventoryObserver {
    void onLowStock(String sku, int remaining);
}
