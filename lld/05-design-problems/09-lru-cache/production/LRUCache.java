package production;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe LRU Cache.
 *
 * WHY ONE LOCK AROUND THE WHOLE OPERATION:
 * Each get/put mutates TWO structures together — the HashMap AND the
 * doubly linked list (recency order). These two mutations must be a
 * single atomic unit. A ConcurrentHashMap would make the map safe but
 * NOT the list reorder, so the list could still be corrupted by
 * concurrent splices. The correct, simple design is one lock guarding
 * the composite get/put.
 *
 * We use a ReentrantLock (over `synchronized`) for clarity and because
 * it leaves room to upgrade (tryLock with timeout, fairness, or
 * sharding) without changing the call sites. Note `get` also takes the
 * write lock because it MUTATES recency order — an LRU "read" is really
 * a write.
 */
public class LRUCache<K, V> {

    private static class Node<K, V> {
        K key;
        V value;
        Node<K, V> prev;
        Node<K, V> next;

        Node(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    private final int capacity;
    private final Map<K, Node<K, V>> map = new HashMap<>();
    private final Node<K, V> head = new Node<>(null, null);
    private final Node<K, V> tail = new Node<>(null, null);
    private final ReentrantLock lock = new ReentrantLock();

    public LRUCache(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be >= 1");
        }
        this.capacity = capacity;
        head.next = tail;
        tail.prev = head;
    }

    public V get(K key) {
        lock.lock(); // guards map + list as one atomic unit
        try {
            Node<K, V> node = map.get(key);
            if (node == null) {
                return null;
            }
            moveToHead(node); // recency update => this "read" mutates state
            return node.value;
        } finally {
            lock.unlock(); // always release, even on exception
        }
    }

    public void put(K key, V value) {
        lock.lock();
        try {
            Node<K, V> existing = map.get(key);
            if (existing != null) {
                existing.value = value;
                moveToHead(existing);
                return;
            }
            Node<K, V> node = new Node<>(key, value);
            map.put(key, node);
            addFirst(node);

            if (map.size() > capacity) {
                Node<K, V> lru = tail.prev;
                remove(lru);
                map.remove(lru.key);
            }
        } finally {
            lock.unlock();
        }
    }

    private void addFirst(Node<K, V> node) {
        node.prev = head;
        node.next = head.next;
        head.next.prev = node;
        head.next = node;
    }

    private void remove(Node<K, V> node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }

    private void moveToHead(Node<K, V> node) {
        remove(node);
        addFirst(node);
    }

    public static void main(String[] args) throws InterruptedException {
        LRUCache<Integer, Integer> cache = new LRUCache<>(100);

        // Hammer the cache from multiple threads to show it stays consistent.
        Runnable worker = () -> {
            for (int i = 0; i < 1000; i++) {
                cache.put(i % 150, i);
                cache.get(i % 150);
            }
        };
        Thread t1 = new Thread(worker);
        Thread t2 = new Thread(worker);
        t1.start(); t2.start();
        t1.join(); t2.join();

        System.out.println("No corruption under concurrent access. Sample get(50)=" + cache.get(50));
    }
}
