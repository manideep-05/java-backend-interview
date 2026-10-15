package normal;

import java.util.HashMap;
import java.util.Map;

/**
 * LRU Cache — single-threaded, O(1) get/put.
 *
 * Data structures (the heart of the design):
 *   - map: key -> Node, for O(1) lookup.
 *   - a doubly linked list of Nodes ordered by recency:
 *       head.next = most-recently-used (MRU)
 *       tail.prev = least-recently-used (LRU, the eviction victim)
 *
 * We use sentinel head/tail nodes to avoid null checks when splicing.
 */
public class LRUCache<K, V> {

    /** Doubly-linked node. Stores the key so eviction can clean the map. */
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
    private final Node<K, V> head = new Node<>(null, null); // sentinel
    private final Node<K, V> tail = new Node<>(null, null); // sentinel

    public LRUCache(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be >= 1");
        }
        this.capacity = capacity;
        head.next = tail;
        tail.prev = head;
    }

    /** O(1): look up, and if found refresh recency by moving to head. */
    public V get(K key) {
        Node<K, V> node = map.get(key);
        if (node == null) {
            return null; // miss
        }
        moveToHead(node);
        return node.value;
    }

    /** O(1): insert/update; evict LRU if over capacity. */
    public void put(K key, V value) {
        Node<K, V> existing = map.get(key);
        if (existing != null) {
            existing.value = value; // update
            moveToHead(existing);   // refresh recency
            return;
        }
        Node<K, V> node = new Node<>(key, value);
        map.put(key, node);
        addFirst(node);

        if (map.size() > capacity) {
            Node<K, V> lru = tail.prev; // the least-recently-used real node
            remove(lru);
            map.remove(lru.key);        // must also drop from the map
        }
    }

    // ----- doubly-linked-list helpers (all O(1)) -----

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

    public static void main(String[] args) {
        LRUCache<Integer, String> cache = new LRUCache<>(2);
        cache.put(1, "one");
        cache.put(2, "two");
        System.out.println(cache.get(1)); // "one" (1 now MRU)
        cache.put(3, "three");            // evicts 2 (LRU)
        System.out.println(cache.get(2)); // null (evicted)
        System.out.println(cache.get(3)); // "three"
    }
}
