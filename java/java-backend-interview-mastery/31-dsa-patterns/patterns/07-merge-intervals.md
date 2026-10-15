# Pattern 07 — Merge Intervals

## Identify it when
- The input is a list of **intervals** `[start, end]` and you must **merge overlaps**, **insert**, **count overlaps**, or detect conflicts.
- Keywords: "meeting rooms", "merge overlapping", "insert interval", "free time", "minimum arrows", "non-overlapping".

**Core idea:** **sort by start** (sometimes by end), then sweep left-to-right merging/comparing adjacent intervals. Two intervals `[a,b]` and `[c,d]` overlap iff `c <= b` (when sorted by start).

## Template
```java
Arrays.sort(iv, (x, y) -> Integer.compare(x[0], y[0]));  // sort by start
List<int[]> out = new ArrayList<>();
for (int[] cur : iv) {
    if (out.isEmpty() || out.get(out.size()-1)[1] < cur[0]) out.add(cur);   // no overlap
    else out.get(out.size()-1)[1] = Math.max(out.get(out.size()-1)[1], cur[1]); // merge
}
```

## Problems

### P1. Merge overlapping intervals
```java
int[][] merge(int[][] iv) {
    Arrays.sort(iv, (a, b) -> Integer.compare(a[0], b[0]));
    List<int[]> out = new ArrayList<>();
    for (int[] cur : iv) {
        if (out.isEmpty() || out.get(out.size()-1)[1] < cur[0]) out.add(cur);
        else out.get(out.size()-1)[1] = Math.max(out.get(out.size()-1)[1], cur[1]);
    }
    return out.toArray(new int[0][]);
}
```
**Why:** after sorting by start, any overlap is with the last merged interval. **Time O(n log n)** (sort dominates), **Space O(n)** output.

### P2. Insert interval into a sorted non-overlapping list
```java
int[][] insert(int[][] iv, int[] ni) {
    List<int[]> out = new ArrayList<>();
    int i = 0, n = iv.length;
    while (i < n && iv[i][1] < ni[0]) out.add(iv[i++]);        // intervals entirely before
    while (i < n && iv[i][0] <= ni[1]) {                       // overlapping -> merge into ni
        ni[0] = Math.min(ni[0], iv[i][0]);
        ni[1] = Math.max(ni[1], iv[i][1]); i++;
    }
    out.add(ni);
    while (i < n) out.add(iv[i++]);                            // intervals entirely after
    return out.toArray(new int[0][]);
}
```
**Why:** input already sorted → no sort needed → **Time O(n), Space O(n).**

### P3. Meeting rooms — can one person attend all? (conflict detection)
```java
boolean canAttendAll(int[][] iv) {
    Arrays.sort(iv, (a, b) -> Integer.compare(a[0], b[0]));
    for (int i = 1; i < iv.length; i++)
        if (iv[i][0] < iv[i-1][1]) return false;   // starts before previous ends -> conflict
    return true;
}
```
**Time O(n log n), Space O(1).**

### P4. Meeting rooms II — minimum rooms needed (max concurrent overlaps)
```java
int minMeetingRooms(int[][] iv) {
    int n = iv.length;
    int[] starts = new int[n], ends = new int[n];
    for (int i = 0; i < n; i++) { starts[i] = iv[i][0]; ends[i] = iv[i][1]; }
    Arrays.sort(starts); Arrays.sort(ends);
    int rooms = 0, best = 0, e = 0;
    for (int s = 0; s < n; s++) {
        while (e < n && ends[e] <= starts[s]) { rooms--; e++; }  // a meeting freed a room
        rooms++;                                                 // this meeting needs a room
        best = Math.max(best, rooms);
    }
    return best;
}
```
**Why:** sweep events; the peak number of simultaneously-active intervals is the answer. (A min-heap of end times is the equivalent O(n log n) approach.) **Time O(n log n), Space O(n).**

### P5. Minimum arrows to burst balloons (greedy by end)
```java
int findMinArrows(int[][] pts) {
    Arrays.sort(pts, (a, b) -> Integer.compare(a[1], b[1]));  // sort by END here
    int arrows = 0; long lastEnd = Long.MIN_VALUE;
    for (int[] p : pts) {
        if (p[0] > lastEnd) { arrows++; lastEnd = p[1]; }      // need a new arrow at this end
    }
    return arrows;
}
```
**Why:** sorting by **end** and shooting at each end bursts the most overlapping balloons greedily. **Time O(n log n), Space O(1).** Note the pattern sometimes sorts by end, not start — decide by what you're optimizing.

## Follow-ups & optimizations
- *"Stream of intervals / can't sort upfront?"* → maintain a `TreeMap` of intervals keyed by start for O(log n) insert-and-merge.
- *"Count total covered length / free gaps?"* → merge first, then sum lengths or complement.
- *"Why sort by end for arrows but start for merge?"* → merging needs start order to detect overlaps; interval-point greedy (arrows/activity selection) needs end order to keep the earliest-finishing choice.
- Edge cases: touching intervals (`[1,2],[2,3]` — do they overlap? clarify inclusive vs exclusive ends).

## Complexity summary
Sort-based: **O(n log n) time, O(n) space.** Insert into pre-sorted: **O(n).** The sort is almost always the dominant cost; the sweep is O(n).
