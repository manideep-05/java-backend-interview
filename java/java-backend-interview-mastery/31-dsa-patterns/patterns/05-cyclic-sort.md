# Pattern 05 — Cyclic Sort

## Identify it when
- The array contains **n numbers in the range [1..n]** (or [0..n]) — a strong, specific signal.
- You must **find missing / duplicate / corrupt** numbers, usually in **O(n) time and O(1) space** (no extra array/HashSet).
- "Numbers from 1 to n", "first missing positive", "find all disappeared numbers".

**Core idea:** each value `v` belongs at index `v-1` (for [1..n]). Walk the array and keep **swapping each element to its correct index** until everything is in place; then the index where `a[i] != i+1` reveals the missing/duplicate.

## Template
```java
int i = 0;
while (i < n) {
    int correct = a[i] - 1;                 // where a[i] should live (for range [1..n])
    if (a[i] >= 1 && a[i] <= n && a[i] != a[correct]) swap(a, i, correct);
    else i++;                               // in place (or out of range) -> advance
}
```
> The loop is O(n) even though it looks nested: each swap puts one number home permanently.

## Problems

### P1. Missing number ([0..n], exactly one missing)
```java
int missingNumber(int[] a) {
    int n = a.length, i = 0;
    while (i < n) {
        int correct = a[i];                 // range [0..n]: value v belongs at index v
        if (a[i] < n && a[i] != a[correct]) swap(a, i, correct);
        else i++;
    }
    for (i = 0; i < n; i++) if (a[i] != i) return i;  // first index out of place
    return n;                                          // all present -> n is missing
}
```
**Time O(n), Space O(1).** (XOR or sum formula also work; cyclic sort generalizes to the harder variants below.)

### P2. Find all numbers disappeared in [1..n]
```java
List<Integer> findDisappeared(int[] a) {
    int n = a.length, i = 0;
    while (i < n) {
        int c = a[i] - 1;
        if (a[i] != a[c]) swap(a, i, c); else i++;
    }
    List<Integer> res = new ArrayList<>();
    for (i = 0; i < n; i++) if (a[i] != i + 1) res.add(i + 1); // index not holding its number
    return res;
}
```
**Time O(n), Space O(1)** (output aside).

### P3. Find the duplicate number (and the missing one)
```java
int[] findErrorNums(int[] a) {              // returns {duplicate, missing}
    int n = a.length, i = 0;
    while (i < n) {
        int c = a[i] - 1;
        if (a[i] != a[c]) swap(a, i, c); else i++;
    }
    for (i = 0; i < n; i++)
        if (a[i] != i + 1) return new int[]{a[i], i + 1}; // a[i] is the dup here, i+1 missing
    return new int[]{-1, -1};
}
```
**Time O(n), Space O(1).**

### P4. First missing positive (hard — classic)
```java
int firstMissingPositive(int[] a) {
    int n = a.length, i = 0;
    while (i < n) {
        int c = a[i] - 1;
        if (a[i] >= 1 && a[i] <= n && a[i] != a[c]) swap(a, i, c); // ignore <=0 and > n
        else i++;
    }
    for (i = 0; i < n; i++) if (a[i] != i + 1) return i + 1;       // first gap
    return n + 1;                                                  // 1..n all present
}
```
**Why it's clever:** only values in [1..n] can be the answer's neighbors; everything else is irrelevant and left in place. **Time O(n), Space O(1)** — beating the O(n) HashSet approach on space.

## Follow-ups & optimizations
- *"Find the duplicate without modifying the array and in O(1) space?"* → that's actually **Floyd's cycle detection** (pattern 04) on the array-as-functional-graph, not cyclic sort — a great "I know multiple patterns" answer.
- *"Numbers in [1..n] but with multiple duplicates?"* → cyclic sort still groups them; adjust the final scan.
- Edge cases: out-of-range values (guard the swap), already-in-place values (advance, don't infinite-loop — the `a[i] != a[c]` check prevents swapping equal values forever).

## Complexity summary
All variants: **O(n) time, O(1) space.** The identifying signal — "n numbers in range [1..n]" — is what makes this pattern worth memorizing; it turns many "use a HashSet" problems into O(1) space.
