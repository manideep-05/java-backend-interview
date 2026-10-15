package production;

/** OBSERVER: notified on a low-stock crossing (same contract as normal). */
public interface InventoryObserver {
    void onLowStock(String sku, int remaining);
}
