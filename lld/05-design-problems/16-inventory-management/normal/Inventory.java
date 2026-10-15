package normal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SUBJECT. Holds stock items and notifies observers on a threshold
 * crossing. Reserve/restock delegate the math to StockItem (SRP).
 */
public class Inventory {
    private final Map<String, StockItem> items = new HashMap<>();
    private final List<InventoryObserver> observers = new ArrayList<>();

    public void addObserver(InventoryObserver o) {
        observers.add(o);
    }

    public void addSku(String sku, int qty, int threshold) {
        items.put(sku, new StockItem(qty, threshold));
    }

    public boolean reserve(String sku, int qty) {
        StockItem item = items.get(sku);
        if (item == null || !item.reserve(qty)) {
            return false;
        }
        if (item.crossedThreshold()) {
            notifyLowStock(sku, item.getQuantity());
        }
        return true;
    }

    public void restock(String sku, int qty) {
        items.get(sku).restock(qty);
    }

    private void notifyLowStock(String sku, int remaining) {
        for (InventoryObserver o : observers) {
            o.onLowStock(sku, remaining);
        }
    }

    public static void main(String[] args) {
        Inventory inventory = new Inventory();
        inventory.addObserver((sku, remaining) ->
                System.out.println("REORDER ALERT: " + sku + " low (" + remaining + " left)"));
        inventory.addSku("widget", 5, 2); // threshold 2

        inventory.reserve("widget", 2); // 3 left
        inventory.reserve("widget", 2); // 1 left -> crosses threshold, alert fires once
        inventory.reserve("widget", 1); // 0 left -> no second alert (latched)
    }
}
