# Pattern 03 — Prefix Sum

## Identify it when
- You need **sum (or count) over many ranges** of an array, or **subarray sums equal to K**.
- The array has **negatives** so sliding window won't work for a sum target.
- You see "number of subarrays with sum = K / divisible by K", "range sum query (immutable)", "subarray with equal 0s and 1s".

**Core idea:** `prefix[i] = a[0] + … + a[i-1]`. Then `sum(i..j) = prefix[j+1] - prefix[i]`. Combine with a **HashMap of prefix→count** to find subarrays in one pass.

## Template
```java
long[] prefix = new long[n + 1];
for (int i = 0; i < n; i++) prefix[i+1] = prefix[i] + a[i];
// range sum [i..j] inclusive = prefix[j+1] - prefix[i]
```

## Problems

### P1. Range sum query — immutable (precompute once, O(1) per query)
```java
class NumArray {
    private final long[] prefix;
    NumArray(int[] a) {
        prefix = new long[a.length + 1];
        for (int i = 0; i < a.length; i++) prefix[i+1] = prefix[i] + a[i];
    }
    int sumRange(int i, int j) { return (int)(prefix[j+1] - prefix[i]); }
}
```
**Why:** build O(n) once; each query O(1). **Time O(n) build + O(1)/query, Space O(n).**

### P2. Subarray sum equals K (count) — prefix + HashMap
```java
int subarraySum(int[] a, int k) {
    Map<Long,Integer> seen = new HashMap<>();  // prefixSum -> how many times
    seen.put(0L, 1);                           // empty prefix
    long run = 0; int count = 0;
    for (int x : a) {
        run += x;
        count += seen.getOrDefault(run - k, 0); // some earlier prefix made (run - prev) = k
        seen.merge(run, 1, Integer::sum);
    }
    return count;
}
```
**Why:** a subarray `(i..j)` sums to K iff `prefix[j+1] - prefix[i] = K`, i.e. `prefix[i] = run - K`. Count how many earlier prefixes equal `run - K`. Works **with negatives** (where sliding window fails). **Time O(n), Space O(n).**

### P3. Contiguous array — longest subarray with equal 0s and 1s
```java
int findMaxLength(int[] a) {
    Map<Integer,Integer> firstIndex = new HashMap<>();
    firstIndex.put(0, -1);                 // running balance 0 "seen" before index 0
    int run = 0, best = 0;
    for (int i = 0; i < a.length; i++) {
        run += (a[i] == 1) ? 1 : -1;        // +1 for 1, -1 for 0
        if (firstIndex.containsKey(run)) best = Math.max(best, i - firstIndex.get(run));
        else firstIndex.put(run, i);        // store first time we saw this balance
    }
    return best;
}
```
**Why:** map 0→−1; equal counts ⟺ the running balance returns to a value seen before; the span between equal balances has equal 0s/1s. **Time O(n), Space O(n).**

### P4. Subarray sums divisible by K (count)
```java
int subarraysDivByK(int[] a, int k) {
    int[] cnt = new int[k];                 // remainder -> count
    cnt[0] = 1;
    int run = 0, ans = 0;
    for (int x : a) {
        run = ((run + x) % k + k) % k;       // normalize negative remainders
        ans += cnt[run];                     // same remainder => difference divisible by k
        cnt[run]++;
    }
    return ans;
}
```
**Why:** two prefixes with the same remainder mod K bound a subarray divisible by K. **Time O(n), Space O(k).**

### P5. 2D range sum (immutable matrix) — prefix sums in 2D
```java
class NumMatrix {
    private final long[][] p;               // p[i][j] = sum of submatrix (0,0)..(i-1,j-1)
    NumMatrix(int[][] m) {
        int R = m.length, C = m[0].length;
        p = new long[R+1][C+1];
        for (int i = 0; i < R; i++)
            for (int j = 0; j < C; j++)
                p[i+1][j+1] = m[i][j] + p[i][j+1] + p[i+1][j] - p[i][j];
    }
    int sumRegion(int r1,int c1,int r2,int c2){
        return (int)(p[r2+1][c2+1] - p[r1][c2+1] - p[r2+1][c1] + p[r1][c1]);
    }
}
```
**Time O(R·C) build + O(1)/query.**

## Follow-ups & optimizations
- *"Mutable array with range sums + point updates?"* → prefix sum is O(n) per update; use a **Fenwick/Binary Indexed Tree** or **Segment Tree** → O(log n) update and query.
- *"Why not sliding window for sum = K?"* → negatives break monotonicity; prefix + HashMap handles them.
- Watch **integer overflow** → use `long` for prefix sums on large inputs.
- Negative remainders in modular problems → normalize with `((x % k) + k) % k`.

## Complexity summary
Range queries: **O(n) build, O(1) query.** Count-subarray problems: **O(n) time, O(n) space** (or O(k) for modular).
