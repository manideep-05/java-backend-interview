# Pattern 06 — Binary Search (and Binary Search on the Answer)

## Identify it when
- The input is **sorted** (or rotated-sorted) and you search for a value / boundary.
- You need the **first/last position**, **insertion point**, or a **boundary** where a condition flips false→true.
- **Binary search on the answer:** the problem asks to **minimize the maximum** (or maximize the minimum / find the smallest X that works), and there's a **monotonic feasibility check** `canDo(x)` — if x works, every larger x works (or vice versa). Input size up to ~10⁹ with an O(n) check is a strong tell.

**Core idea:** halve a monotonic search space each step → O(log) on the space.

## Templates
```java
// Classic exact search
int lo = 0, hi = n - 1;
while (lo <= hi) {
    int mid = lo + (hi - lo) / 2;          // avoid overflow (not (lo+hi)/2)
    if (a[mid] == target) return mid;
    if (a[mid] < target) lo = mid + 1; else hi = mid - 1;
}
return -1;

// Lower-bound / first index where predicate(x) is true (half-open [lo,hi))
int lo = 0, hi = n;                         // hi exclusive
while (lo < hi) {
    int mid = lo + (hi - lo) / 2;
    if (predicate(mid)) hi = mid; else lo = mid + 1;
}
return lo;                                   // first true (or n if none)
```

## Problems

### P1. First and last position of a target (boundaries)
```java
int[] searchRange(int[] a, int target) {
    int left = lowerBound(a, target);
    if (left == a.length || a[left] != target) return new int[]{-1, -1};
    int right = lowerBound(a, target + 1) - 1;    // first index > target, minus 1
    return new int[]{left, right};
}
int lowerBound(int[] a, int t) {                   // first index with a[i] >= t
    int lo = 0, hi = a.length;
    while (lo < hi) { int m = lo + (hi - lo)/2; if (a[m] < t) lo = m + 1; else hi = m; }
    return lo;
}
```
**Why:** reduce "range" to two lower-bound queries. **Time O(log n), Space O(1).**

### P2. Search in a rotated sorted array
```java
int search(int[] a, int target) {
    int lo = 0, hi = a.length - 1;
    while (lo <= hi) {
        int m = lo + (hi - lo)/2;
        if (a[m] == target) return m;
        if (a[lo] <= a[m]) {                       // left half is sorted
            if (a[lo] <= target && target < a[m]) hi = m - 1; else lo = m + 1;
        } else {                                   // right half is sorted
            if (a[m] < target && target <= a[hi]) lo = m + 1; else hi = m - 1;
        }
    }
    return -1;
}
```
**Why:** one half is always sorted; decide which side can contain the target. **Time O(log n), Space O(1).**

### P3. Binary search on the answer — Koko eating bananas (minimize max rate)
```java
int minEatingSpeed(int[] piles, int h) {
    int lo = 1, hi = Arrays.stream(piles).max().getAsInt();
    while (lo < hi) {
        int mid = lo + (hi - lo)/2;
        if (hoursNeeded(piles, mid) <= h) hi = mid;  // feasible -> try smaller
        else lo = mid + 1;                           // too slow -> go faster
    }
    return lo;                                       // smallest feasible speed
}
long hoursNeeded(int[] piles, int speed) {
    long h = 0; for (int p : piles) h += (p + speed - 1L) / speed; return h; // ceil div
}
```
**Why:** feasibility is monotonic — if speed `s` finishes in time, any `s' > s` does too — so binary-search the smallest feasible speed. **Time O(n log maxPile), Space O(1).** This "monotonic predicate" framing is the key SDE-2 insight; also solves "split array largest sum", "ship within D days", "min capacity".

### P4. Find minimum in a rotated sorted array
```java
int findMin(int[] a) {
    int lo = 0, hi = a.length - 1;
    while (lo < hi) {
        int m = lo + (hi - lo)/2;
        if (a[m] > a[hi]) lo = m + 1;      // min is to the right of m
        else hi = m;                       // min is at m or to the left
    }
    return a[lo];
}
```
**Time O(log n), Space O(1).**

### P5. Median of two sorted arrays (hard — partition binary search)
```java
double findMedianSortedArrays(int[] A, int[] B) {
    if (A.length > B.length) return findMedianSortedArrays(B, A); // binary search on smaller
    int m = A.length, n = B.length, half = (m + n + 1) / 2;
    int lo = 0, hi = m;
    while (lo <= hi) {
        int i = (lo + hi) / 2, j = half - i;
        int Aleft = i == 0 ? Integer.MIN_VALUE : A[i-1];
        int Aright = i == m ? Integer.MAX_VALUE : A[i];
        int Bleft = j == 0 ? Integer.MIN_VALUE : B[j-1];
        int Bright = j == n ? Integer.MAX_VALUE : B[j];
        if (Aleft <= Bright && Bleft <= Aright) {        // correct partition
            if (((m + n) & 1) == 1) return Math.max(Aleft, Bleft);
            return (Math.max(Aleft, Bleft) + Math.min(Aright, Bright)) / 2.0;
        } else if (Aleft > Bright) hi = i - 1; else lo = i + 1;
    }
    return 0.0;
}
```
**Why:** binary-search the partition of the smaller array so left halves hold the lower `half` elements. **Time O(log(min(m,n))), Space O(1).**

## Follow-ups & optimizations
- *Overflow:* use `lo + (hi - lo)/2`, not `(lo + hi)/2`.
- *Off-by-one:* pick one convention (half-open `[lo, hi)` with lower-bound) and reuse it everywhere — most binary-search bugs are boundary bugs.
- *"How did you know to binary-search the answer?"* → "the feasibility check is monotonic and the answer range is huge, so I search the answer space, not the array."
- *Duplicates in rotated array?* → worst case degrades to O(n) when you can't tell which half is sorted (shrink `hi--` when `a[lo]==a[m]==a[hi]`).

## Complexity summary
Classic/rotated: **O(log n) time, O(1) space.** Binary-search-on-answer: **O(n log(range)) time** (n per feasibility check), **O(1) space.**
