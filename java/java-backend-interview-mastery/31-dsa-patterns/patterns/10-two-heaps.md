# Pattern 10 — Two Heaps

## Identify it when
- You need the **median of a stream** (or a sliding window), or to keep a dataset **balanced into a lower and upper half** with fast access to the middle.
- "Find median from data stream", "sliding window median", "IPO / maximize capital", "schedule with two priorities".

**Core idea:** keep a **max-heap for the lower half** and a **min-heap for the upper half**, balanced so their sizes differ by at most 1. The median is then the top of one heap (odd count) or the average of both tops (even count) — all in O(1) read, O(log n) insert.

## Template
```java
PriorityQueue<Integer> lo = new PriorityQueue<>(Collections.reverseOrder()); // max-heap (lower half)
PriorityQueue<Integer> hi = new PriorityQueue<>();                           // min-heap (upper half)

void add(int x) {
    lo.offer(x);                 // 1) always push to lo
    hi.offer(lo.poll());         // 2) move lo's max to hi (keeps order across heaps)
    if (hi.size() > lo.size()) lo.offer(hi.poll());  // 3) rebalance so lo >= hi in size
}
double median() {
    return lo.size() > hi.size() ? lo.peek() : (lo.peek() + hi.peek()) / 2.0;
}
```

## Problems

### P1. Find median from a data stream (classic)
```java
class MedianFinder {
    private final PriorityQueue<Integer> lo = new PriorityQueue<>(Collections.reverseOrder());
    private final PriorityQueue<Integer> hi = new PriorityQueue<>();
    void addNum(int num) {
        lo.offer(num);
        hi.offer(lo.poll());                      // balance values across the two heaps
        if (hi.size() > lo.size()) lo.offer(hi.poll());
    }
    double findMedian() {
        return lo.size() > hi.size() ? lo.peek() : (lo.peek() + hi.peek()) / 2.0;
    }
}
```
**Why:** `lo` holds the smaller half (max on top), `hi` the larger half (min on top); the median sits at the boundary. The push-to-lo → move-to-hi → rebalance dance keeps both ordering and sizes correct. **add O(log n), findMedian O(1), Space O(n).**

### P2. IPO — maximize capital with K projects (two heaps / heap + sort)
```java
int findMaximizedCapital(int k, int w, int[] profits, int[] capital) {
    int n = profits.length;
    int[][] proj = new int[n][2];
    for (int i = 0; i < n; i++) { proj[i][0] = capital[i]; proj[i][1] = profits[i]; }
    Arrays.sort(proj, (a, b) -> Integer.compare(a[0], b[0]));  // by capital required
    PriorityQueue<Integer> affordableProfits = new PriorityQueue<>(Collections.reverseOrder());
    int i = 0;
    for (int c = 0; c < k; c++) {
        while (i < n && proj[i][0] <= w) affordableProfits.offer(proj[i++][1]); // unlock affordable
        if (affordableProfits.isEmpty()) break;
        w += affordableProfits.poll();            // greedily take the most profitable affordable one
    }
    return w;
}
```
**Why:** a sorted pointer unlocks projects you can now afford; a max-heap picks the best among them. **Time O(n log n), Space O(n).**

### P3. Sliding window median (two heaps + lazy deletion)
```java
// Sketch: maintain two heaps for the window; when an element leaves the window,
// mark it for lazy removal (a HashMap of pending deletions) and prune heap tops
// before reading them, rebalancing sizes after each add/remove.
```
**Why / note:** heaps can't delete an arbitrary interior element in O(log n), so use **lazy deletion** — defer removal and skip "dead" entries when they reach the top. **Each step O(log k), Space O(k).** State the lazy-deletion idea even if you don't code it fully.

## Follow-ups & optimizations
- *"Median with a sliding window — why is it harder?"* → you must remove the element leaving the window; heaps don't support arbitrary delete, so you use lazy deletion or a `TreeMap`/multiset (`TreeMap<value,count>`) which supports O(log n) removal and order statistics.
- *"Why two heaps instead of a sorted list?"* → inserting into a sorted array is O(n); two heaps give O(log n) insert with O(1) median read.
- *Balancing invariant:* decide and state it — e.g., `lo.size() == hi.size()` or `lo.size() == hi.size() + 1`. Off-by-one here is the usual bug.
- *Overflow:* average two ints as `(a + b) / 2.0` with care, or use `long`.

## Complexity summary
Stream median: **add O(log n), read O(1), space O(n).** Sliding-window median: **O(log k) per step** with lazy deletion or a TreeMap multiset. The pattern's value is O(1) access to the middle of a dynamic dataset.
