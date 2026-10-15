package normal;

import java.util.HashMap;
import java.util.Map;

/**
 * In-memory KV store with TTL — single-threaded, LAZY expiry.
 *
 * Each entry stores its value and an expiry timestamp (0 = never). On get,
 * if the entry is past expiry we remove it and report a miss. Simple, no
 * background work; downside is unread expired keys linger (production adds
 * a sweeper).
 */
public class KeyValueStore {

    private static class Entry {
        final String value;
        final long expiryNanos; // 0 => never expires

        Entry(String value, long expiryNanos) {
            this.value = value;
            this.expiryNanos = expiryNanos;
        }
        boolean isExpired(long now) {
            return expiryNanos != 0 && now > expiryNanos;
        }
    }

    private final Map<String, Entry> map = new HashMap<>();

    public void put(String key, String value) {
        map.put(key, new Entry(value, 0)); // no TTL
    }

    public void put(String key, String value, long ttlMillis) {
        long expiry = System.nanoTime() + ttlMillis * 1_000_000L; // monotonic
        map.put(key, new Entry(value, expiry));
    }

    public String get(String key) {
        Entry e = map.get(key);
        if (e == null) {
            return null;
        }
        if (e.isExpired(System.nanoTime())) {
            map.remove(key);  // lazy expiry
            return null;
        }
        return e.value;
    }

    public void delete(String key) {
        map.remove(key);
    }

    public static void main(String[] args) throws InterruptedException {
        KeyValueStore store = new KeyValueStore();
        store.put("a", "1");             // no TTL
        store.put("b", "2", 50);         // expires in 50ms
        System.out.println("a=" + store.get("a")); // 1
        System.out.println("b=" + store.get("b")); // 2
        Thread.sleep(80);
        System.out.println("b after ttl=" + store.get("b")); // null (expired)
    }
}
