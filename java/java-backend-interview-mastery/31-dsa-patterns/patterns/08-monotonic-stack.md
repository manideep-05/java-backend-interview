# Pattern 08 — Monotonic Stack (and Monotonic Deque)

## Identify it when
- You need the **next/previous greater or smaller element** for every item.
- "Largest rectangle in histogram", "daily temperatures", "stock span", "trapping rain water", "remove k digits to make smallest".
- A brute force is O(n²) comparing each element to others, and you can resolve each element against a **stack that stays sorted**.
- **Monotonic deque** variant: **sliding window maximum/minimum** (window max in O(1) amortized).

**Core idea:** keep a stack whose values are monotonic (increasing or decreasing). When a new element breaks the order, **pop** — each pop resolves an answer. Each element is pushed and popped once → O(n).

## Template
```java
Deque<Integer> stack = new ArrayDeque<>();   // store indices, values monotonic
for (int i = 0; i < n; i++) {
    while (!stack.isEmpty() && a[stack.peek()] < a[i]) {   // decreasing stack
        int idx = stack.pop();
        // a[i] is the "next greater" of a[idx]; (i - idx) is the span
    }
    stack.push(i);
}
```

## Problems

### P1. Daily temperatures (next warmer day = next greater)
```java
int[] dailyTemperatures(int[] t) {
    int n = t.length; int[] res = new int[n];
    Deque<Integer> st = new ArrayDeque<>();      // indices, decreasing temps
    for (int i = 0; i < n; i++) {
        while (!st.isEmpty() && t[st.peek()] < t[i]) {
            int j = st.pop();
            res[j] = i - j;                       // days until a warmer temperature
        }
        st.push(i);
    }
    return res;
}
```
**Why:** when a warmer day appears, it's the "next greater" for everything cooler still on the stack. **Time O(n)** (each index pushed/popped once), **Space O(n).**

### P2. Next greater element (circular)
```java
int[] nextGreaterCircular(int[] a) {
    int n = a.length; int[] res = new int[n]; Arrays.fill(res, -1);
    Deque<Integer> st = new ArrayDeque<>();
    for (int i = 0; i < 2 * n; i++) {             // go around twice for circularity
        int cur = a[i % n];
        while (!st.isEmpty() && a[st.peek()] < cur) res[st.pop()] = cur;
        if (i < n) st.push(i);
    }
    return res;
}
```
**Time O(n), Space O(n).**

### P3. Largest rectangle in histogram (classic hard)
```java
int largestRectangleArea(int[] h) {
    int n = h.length, best = 0;
    Deque<Integer> st = new ArrayDeque<>();       // increasing heights
    for (int i = 0; i <= n; i++) {
        int cur = (i == n) ? 0 : h[i];            // sentinel 0 flushes the stack
        while (!st.isEmpty() && h[st.peek()] > cur) {
            int height = h[st.pop()];
            int left = st.isEmpty() ? -1 : st.peek();
            best = Math.max(best, height * (i - left - 1)); // width between boundaries
        }
        st.push(i);
    }
    return best;
}
```
**Why:** each bar's maximal rectangle extends until a shorter bar on each side; the stack finds both boundaries in one pass. **Time O(n), Space O(n).**

### P4. Trapping rain water (monotonic stack variant)
```java
int trap(int[] h) {
    Deque<Integer> st = new ArrayDeque<>(); int water = 0;
    for (int i = 0; i < h.length; i++) {
        while (!st.isEmpty() && h[i] > h[st.peek()]) {
            int bottom = st.pop();
            if (st.isEmpty()) break;
            int left = st.peek();
            int width = i - left - 1;
            int bounded = Math.min(h[left], h[i]) - h[bottom];
            water += width * bounded;
        }
        st.push(i);
    }
    return water;
}
```
**Time O(n), Space O(n).** (Two-pointer solution does it in O(1) space — a good follow-up to mention.)

### P5. Sliding window maximum (monotonic DEQUE)
```java
int[] maxSlidingWindow(int[] a, int k) {
    int n = a.length; int[] res = new int[n - k + 1];
    Deque<Integer> dq = new ArrayDeque<>();        // indices, decreasing values
    for (int i = 0; i < n; i++) {
        while (!dq.isEmpty() && dq.peekFirst() <= i - k) dq.pollFirst(); // drop out-of-window
        while (!dq.isEmpty() && a[dq.peekLast()] <= a[i]) dq.pollLast(); // keep decreasing
        dq.offerLast(i);
        if (i >= k - 1) res[i - k + 1] = a[dq.peekFirst()];             // front = window max
    }
    return res;
}
```
**Why:** the deque front always holds the current window's max; smaller/older elements can never be the max again so they're discarded. **Time O(n)** (amortized), **Space O(k)** — beats a heap's O(n log k).

## Follow-ups & optimizations
- *Increasing vs decreasing stack?* → use a **decreasing** stack to find next-greater, **increasing** for next-smaller. State which and why.
- *Store indices, not values* → you usually need positions/spans, and indices let you compute widths.
- *"Trapping rain water in O(1) space?"* → two pointers tracking leftMax/rightMax.
- *Sliding-window max with a heap?* → O(n log k) and needs lazy deletion; the deque is the optimal answer.

## Complexity summary
All variants: **O(n) time** (each element pushed and popped at most once), **O(n)** or **O(k)** space. The "each element touched twice" argument is the key thing to say when justifying O(n).
