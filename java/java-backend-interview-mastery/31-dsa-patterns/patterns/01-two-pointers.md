# Pattern 01 — Two Pointers

## Identify it when
- The input is **sorted** (or can be sorted) and you need a **pair/triplet** meeting a sum/difference condition.
- You must process from **both ends inward** (palindrome check, container with most water, reverse).
- You need to **remove/dedup/partition in place** with O(1) extra space (same-direction read/write pointers).
- Brute force is O(n²) nested loops over the array and the data has structure (sorted/paired) you can exploit to drop a loop.

**Two sub-flavors:** *opposite ends* (left/right converge) and *same direction* (slow/fast, read/write).

## Template
```java
// Opposite ends on a sorted array
int l = 0, r = n - 1;
while (l < r) {
    int sum = a[l] + a[r];
    if (sum == target) { /* found */ l++; r--; }
    else if (sum < target) l++;   // need bigger -> move left up
    else r--;                     // need smaller -> move right down
}
```

## Problems

### P1. Pair sum in a sorted array (Two Sum II)
```java
int[] twoSumSorted(int[] a, int target) {
    int l = 0, r = a.length - 1;
    while (l < r) {
        int s = a[l] + a[r];
        if (s == target) return new int[]{l, r};
        if (s < target) l++; else r--;
    }
    return new int[]{-1, -1};
}
```
**Why:** sorted → if the sum is too small the only way to grow it is to raise the left pointer; too big → lower the right. Each step eliminates one candidate. **Time O(n), Space O(1)** (vs O(n) HashMap for the unsorted Two Sum).

### P2. 3Sum (triplets summing to 0)
```java
List<List<Integer>> threeSum(int[] a) {
    Arrays.sort(a);                       // sort enables two-pointer inner scan
    List<List<Integer>> res = new ArrayList<>();
    for (int i = 0; i < a.length - 2; i++) {
        if (i > 0 && a[i] == a[i-1]) continue;          // skip duplicate anchors
        int l = i + 1, r = a.length - 1;
        while (l < r) {
            int s = a[i] + a[l] + a[r];
            if (s == 0) {
                res.add(List.of(a[i], a[l], a[r]));
                while (l < r && a[l] == a[l+1]) l++;     // skip dup
                while (l < r && a[r] == a[r-1]) r--;
                l++; r--;
            } else if (s < 0) l++; else r--;
        }
    }
    return res;
}
```
**Why:** fix one element, two-pointer the rest. **Time O(n²)** (sort O(n log n) + n scans of O(n)), **Space O(1)** extra (ignoring output). The duplicate-skipping is the part interviewers check.

### P3. Container with most water (opposite ends)
```java
int maxArea(int[] h) {
    int l = 0, r = h.length - 1, best = 0;
    while (l < r) {
        best = Math.max(best, Math.min(h[l], h[r]) * (r - l));
        if (h[l] < h[r]) l++; else r--;   // move the shorter wall inward (only chance to improve)
    }
    return best;
}
```
**Why:** area is bounded by the shorter wall; widening is impossible, so moving the taller wall can never help — move the shorter. **Time O(n), Space O(1).**

### P4. Remove duplicates from sorted array in place (same-direction)
```java
int removeDuplicates(int[] a) {
    int write = 0;                        // slow pointer = next unique slot
    for (int read = 0; read < a.length; read++)   // fast pointer scans
        if (read == 0 || a[read] != a[read-1]) a[write++] = a[read];
    return write;                         // new length
}
```
**Why:** a read pointer scans; a write pointer places kept elements. **Time O(n), Space O(1).**

### P5. Valid palindrome (opposite ends, skip non-alnum)
```java
boolean isPalindrome(String s) {
    int l = 0, r = s.length() - 1;
    while (l < r) {
        while (l < r && !Character.isLetterOrDigit(s.charAt(l))) l++;
        while (l < r && !Character.isLetterOrDigit(s.charAt(r))) r--;
        if (Character.toLowerCase(s.charAt(l++)) != Character.toLowerCase(s.charAt(r--))) return false;
    }
    return true;
}
```
**Time O(n), Space O(1).**

## Follow-ups & optimizations
- *"What if the array isn't sorted?"* → sorting costs O(n log n); if you only need existence of a pair, a **HashMap** gives O(n) time / O(n) space (the unsorted Two Sum) — a classic trade-off to state.
- *"4Sum / k-Sum?"* → generalize: fix (k−2) nested loops + two pointers → O(n^(k−1)).
- *"Count pairs, not find one?"* → two pointers still work on sorted; each move accounts for a range of pairs.
- *Duplicates* are the #1 bug: always say how you skip them.

## Complexity summary
Pair/palindrome/container: **O(n) time, O(1) space.** 3Sum: **O(n²) time, O(1) extra.**
