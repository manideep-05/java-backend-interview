package production;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PRODUCTION subject.
 *
 * Concurrency design:
 *  - items: ConcurrentHashMap keyed by SKU.
 *  - observers: CopyOnWriteArrayList — rarely changes, iterated on each
 *    low-stock event, so copy-on-write gives lock-free notification.
 *  - No inventory-wide lock: each StockItem's CAS reserve is the only
 *    synchronization needed, so orders for different SKUs never block,
 *    and orders for the same SKU are arbitrated by CAS (no oversell).
 *  - Observer notification is kept cheap; a slow subscriber should be
 *    made async (see Notification problem) so it doesn't block reserve().
 */
public class Inventory {
    private final Map<String, StockItem> items = new ConcurrentHashMap<>();
    private final List<InventoryObserver> observers = new CopyOnWriteArrayList<>();

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

    public static void main(String[] args) throws InterruptedException {
        Inventory inventory = new Inventory();
        AtomicInteger alerts = new AtomicInteger();
        inventory.addObserver((sku, remaining) -> alerts.incrementAndGet());
        inventory.addSku("widget", 1000, 100);

        AtomicInteger reserved = new AtomicInteger();
        // 10 threads each reserve 100 units of 1000 total -> exactly 1000 units reserve-able.
        Runnable buyer = () -> {
            for (int i = 0; i < 100; i++) {
                if (inventory.reserve("widget", 1)) {
                    reserved.incrementAndGet();
                }
            }
        };
        Thread[] ts = new Thread[10];
        for (int i = 0; i < ts.length; i++) {
            ts[i] = new Thread(buyer);
            ts[i].start();
        }
        for (Thread t : ts) {
            t.join();
        }
        // Reserved must be exactly 1000 (never oversold), and exactly ONE
        // low-stock alert fired on the threshold crossing.
        System.out.println("Reserved (must be 1000): " + reserved.get()
                + ", alerts fired (must be 1): " + alerts.get());
    }
}
