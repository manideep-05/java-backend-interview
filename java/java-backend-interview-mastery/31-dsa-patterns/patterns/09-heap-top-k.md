# Pattern 09 — Top-K / Heap (Priority Queue)

## Identify it when
- The problem asks for the **K largest / smallest / most frequent / closest** elements.
- You need the **running max/min** of a changing set, or to process a **stream** where you can't sort everything.
- Keywords: "top K", "K closest", "K most frequent", "kth largest", "merge K sorted".

**Core idea:** a heap gives O(log n) insert/extract of the min or max. For "K largest", keep a **min-heap of size K** (the smallest of your K bests sits on top, ready to be evicted). For "K smallest", keep a **max-heap of size K**.

## Template
```java
// K largest: min-heap of size K
PriorityQueue<Integer> heap = new PriorityQueue<>();  // min-heap by default
for (int x : a) {
    heap.offer(x);
    if (heap.size() > k) heap.poll();   // evict the smallest -> keeps the K largest
}
// heap now holds the K largest; heap.peek() is the kth largest
```

## Problems

### P1. Kth largest element
```java
int findKthLargest(int[] a, int k) {
    PriorityQueue<Integer> heap = new PriorityQueue<>();   // min-heap of size k
    for (int x : a) { heap.offer(x); if (heap.size() > k) heap.poll(); }
    return heap.peek();
}
```
**Why:** keep only the K largest; the top of the size-K min-heap is the kth largest. **Time O(n log k), Space O(k)** — better than sorting (O(n log n)) when k ≪ n. *(Quickselect gives O(n) average, O(n²) worst — mention it as the follow-up.)*

### P2. K closest points to the origin
```java
int[][] kClosest(int[][] pts, int k) {
    PriorityQueue<int[]> heap = new PriorityQueue<>(                 // max-heap by distance
        (p, q) -> Long.compare(dist(q), dist(p)));
    for (int[] p : pts) { heap.offer(p); if (heap.size() > k) heap.poll(); }
    return heap.toArray(new int[0][]);
}
long dist(int[] p) { return (long)p[0]*p[0] + (long)p[1]*p[1]; }     // no sqrt needed
```
**Why:** a max-heap of size K keeps the K smallest distances (evict the farthest). **Time O(n log k), Space O(k).**

### P3. Top K frequent elements
```java
List<Integer> topKFrequent(int[] a, int k) {
    Map<Integer,Integer> freq = new HashMap<>();
    for (int x : a) freq.merge(x, 1, Integer::sum);
    PriorityQueue<Map.Entry<Integer,Integer>> heap =
        new PriorityQueue<>(Map.Entry.comparingByValue());          // min-heap by frequency
    for (var e : freq.entrySet()) { heap.offer(e); if (heap.size() > k) heap.poll(); }
    List<Integer> res = new ArrayList<>();
    while (!heap.isEmpty()) res.add(heap.poll().getKey());
    return res;
}
```
**Why:** count frequencies, then keep the K most frequent in a size-K heap. **Time O(n log k), Space O(n).** *(Bucket sort by frequency gives O(n) — strong optimization to mention.)*

### P4. Merge K sorted lists
```java
ListNode mergeKLists(ListNode[] lists) {
    PriorityQueue<ListNode> heap = new PriorityQueue<>((x, y) -> Integer.compare(x.val, y.val));
    for (ListNode l : lists) if (l != null) heap.offer(l);
    ListNode dummy = new ListNode(0), tail = dummy;
    while (!heap.isEmpty()) {
        ListNode n = heap.poll();
        tail.next = n; tail = n;
        if (n.next != null) heap.offer(n.next);     // push the next node of that list
    }
    return dummy.next;
}
```
**Why:** the heap holds one candidate per list (the current heads); always extend with the global minimum. **Time O(N log k)** (N total nodes, k lists), **Space O(k).**

### P5. Kth largest in a stream (online, add() returns kth largest)
```java
class KthLargest {
    private final PriorityQueue<Integer> heap = new PriorityQueue<>();
    private final int k;
    KthLargest(int k, int[] nums) { this.k = k; for (int x : nums) add(x); }
    int add(int val) {
        heap.offer(val);
        if (heap.size() > k) heap.poll();
        return heap.peek();                          // kth largest so far
    }
}
```
**Why:** maintain the K largest incrementally. **Each add O(log k), Space O(k).** Streaming is the signal a heap (not sorting) is required.

## Follow-ups & optimizations
- *"Kth largest, best possible time?"* → **Quickselect** averages O(n) (partition like quicksort, recurse one side); worst case O(n²) unless you randomize the pivot.
- *"Top-K frequent in O(n)?"* → **bucket sort** by frequency (frequencies are bounded by n).
- *Heapify vs K inserts:* building a heap from n elements is O(n); extracting K gives O(n + k log n) — compare to the size-K-heap O(n log k) depending on k vs n.
- *Min-heap for K largest* trips people up — be ready to explain why (you evict the smallest of your current bests).

## Complexity summary
Top-K via size-K heap: **O(n log k) time, O(k) space.** Merge K lists: **O(N log k).** Quickselect alternative: **O(n) average.** Streaming: **O(log k) per add.**
