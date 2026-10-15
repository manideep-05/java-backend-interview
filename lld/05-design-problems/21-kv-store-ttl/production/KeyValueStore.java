package production;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PRODUCTION KV store with TTL.
 *
 * Concurrency design:
 *  - ConcurrentHashMap: thread-safe O(1) get/put/remove without a global lock.
 *  - LAZY expiry on get() uses an ATOMIC CONDITIONAL REMOVE:
 *      map.remove(key, staleEntry)
 *    This only deletes if the mapped value is STILL the stale entry. Without
 *    it, a blind map.remove(key) could wipe out a value another thread just
 *    put() under the same key between our read and our remove (the get/expiry
 *    race). The conditional remove closes that race.
 *  - ACTIVE sweeper: a daemon thread periodically removes expired entries so
 *    unread expired keys don't leak memory. (Redis = lazy + sampling sweep.)
 *  - System.nanoTime() for monotonic expiry math (immune to clock jumps).
 */
public class KeyValueStore {

    private static final class Entry {
        final String value;
        final long expiryNanos; // 0 => never
        Entry(String value, long expiryNanos) {
            this.value = value;
            this.expiryNanos = expiryNanos;
        }
        boolean isExpired(long now) { return expiryNanos != 0 && now > expiryNanos; }
    }

    private final Map<String, Entry> map = new ConcurrentHashMap<>();
    private final Thread sweeper;
    private volatile boolean running = true;

    public KeyValueStore() {
        sweeper = new Thread(this::sweepLoop, "kv-sweeper");
        sweeper.setDaemon(true);
        sweeper.start();
    }

    public void put(String key, String value) {
        map.put(key, new Entry(value, 0));
    }

    public void put(String key, String value, long ttlMillis) {
        long expiry = System.nanoTime() + ttlMillis * 1_000_000L;
        map.put(key, new Entry(value, expiry));
    }

    public String get(String key) {
        Entry e = map.get(key);
        if (e == null) {
            return null;
        }
        if (e.isExpired(System.nanoTime())) {
            // Conditional remove: only delete if `e` is still mapped (not a
            // fresh value put by another thread). Atomic in ConcurrentHashMap.
            map.remove(key, e);
            return null;
        }
        return e.value;
    }

    public void delete(String key) {
        map.remove(key);
    }

    private void sweepLoop() {
        while (running) {
            long now = System.nanoTime();
            // Reclaim memory from expired-but-unread keys. remove(k,v) is
            // atomic, so we never drop a freshly-written value.
            for (Map.Entry<String, Entry> entry : map.entrySet()) {
                if (entry.getValue().isExpired(now)) {
                    map.remove(entry.getKey(), entry.getValue());
                }
            }
            try {
                Thread.sleep(50); // sweep interval
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    public void shutdown() {
        running = false;
        sweeper.interrupt();
    }

    public int size() { return map.size(); }

    public static void main(String[] args) throws InterruptedException {
        KeyValueStore store = new KeyValueStore();
        store.put("a", "1");
        store.put("b", "2", 50);
        System.out.println("b=" + store.get("b")); // 2
        Thread.sleep(150);                           // let TTL pass + sweeper run
        System.out.println("b after ttl=" + store.get("b")); // null
        System.out.println("size (b reclaimed by sweeper): " + store.size()); // 1 (only 'a')
        store.shutdown();
    }
}
