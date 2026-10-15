# Pattern 20 — Bit Manipulation

## Identify it when
- The problem involves **toggling/counting bits**, finding a number that **appears once/odd times**, generating **subsets via bitmasks**, or you're asked to avoid extra space using bit tricks.
- "Single number", "number of 1 bits", "counting bits", "missing number", "power of two", "subsets", "bitmask DP".
- Constraints hint at it: "O(1) space", "without using +/-", small set size (≤ ~20 → bitmask over subsets).

**Core identities to know cold:**
- `x & (x - 1)` clears the lowest set bit (counts/removes bits).
- `x & (-x)` isolates the lowest set bit.
- `a ^ a = 0`, `a ^ 0 = a`, XOR is commutative/associative → cancels pairs.
- `x & (1 << i)` tests bit i; `x | (1 << i)` sets it; `x ^ (1 << i)` toggles it.
- `(x >> i) & 1` reads bit i.

## Problems

### P1. Single number (every element twice except one) — XOR cancellation
```java
int singleNumber(int[] a) {
    int x = 0;
    for (int v : a) x ^= v;      // pairs cancel to 0; the unique value remains
    return x;
}
```
**Why:** XOR of a value with itself is 0, so all duplicates vanish, leaving the single element. **Time O(n), Space O(1)** — beats the O(n)-space HashSet approach.

### P2. Number of 1 bits (Hamming weight)
```java
int hammingWeight(int n) {
    int count = 0;
    while (n != 0) { n &= (n - 1); count++; }   // clear lowest set bit each step
    return count;
}
```
**Why:** `n & (n-1)` removes exactly one set bit per iteration, so it loops only as many times as there are 1-bits. **Time O(#set bits), Space O(1).**

### P3. Counting bits (0..n) — DP on bits
```java
int[] countBits(int n) {
    int[] dp = new int[n + 1];
    for (int i = 1; i <= n; i++) dp[i] = dp[i >> 1] + (i & 1); // bits(i) = bits(i/2) + lastBit
    return dp;
}
```
**Why:** `i` has the same bits as `i/2` plus its last bit. **Time O(n), Space O(n).**

### P4. Missing number (XOR trick, [0..n])
```java
int missingNumber(int[] a) {
    int x = a.length;                           // start with n (index that has no value)
    for (int i = 0; i < a.length; i++) x ^= i ^ a[i];  // XOR all indices and values
    return x;                                   // the missing number survives
}
```
**Why:** XOR every index 0..n with every value; matched pairs cancel, leaving the missing one. **Time O(n), Space O(1).**

### P5. Subsets via bitmask (iterative power set)
```java
List<List<Integer>> subsets(int[] a) {
    int n = a.length;
    List<List<Integer>> res = new ArrayList<>();
    for (int mask = 0; mask < (1 << n); mask++) {   // every mask = one subset
        List<Integer> sub = new ArrayList<>();
        for (int i = 0; i < n; i++)
            if ((mask & (1 << i)) != 0) sub.add(a[i]); // bit i set => include a[i]
        res.add(sub);
    }
    return res;
}
```
**Why:** each of the `2^n` masks encodes which elements are in the subset. **Time O(2^n · n), Space O(1)** extra. This bitmask-over-subsets idea also powers **bitmask DP** (e.g., traveling salesman, assignment problems) where the state is "set of used elements".

## Follow-ups & optimizations
- *"Single number where every element appears **three** times except one?"* → can't use plain XOR; count bits mod 3 across all 32 bit positions (or use two accumulators `ones`/`twos`).
- *"Power of two?"* → `n > 0 && (n & (n - 1)) == 0` (exactly one set bit).
- *"Swap without a temp?"* → `a ^= b; b ^= a; a ^= b;` (interview trivia; rarely useful in real code).
- *Overflow / sign:* Java `int` is 32-bit signed; use `>>>` (unsigned right shift) when you don't want sign extension; use `Integer.bitCount`/`Long.bitCount` in real code.
- *Bitmask DP* appears when n ≤ ~20 and the state is "subset of items used".

## Complexity summary
XOR/bit-count tricks: **O(n) time, O(1) space** — the space win is the point. Subset enumeration via masks: **O(2^n · n)**. The identifying signals are "appears once/odd times" (XOR), "O(1) space on a counting problem", and "subsets of a small set" (bitmask).
