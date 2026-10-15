# Pattern 02 — Sliding Window

## Identify it when
- The problem asks about a **contiguous** subarray or substring (not subsequence).
- You want the **longest / shortest / count** of windows satisfying a condition ("at most K distinct", "sum ≥ target", "no repeating chars").
- Brute force is O(n²)/O(n³) over all subarrays and you can avoid recomputing by **sliding** a window: add on the right, remove from the left.

**Two flavors:** *fixed-size* window (window length given) and *variable-size* window (grow right, shrink left while a condition is violated/until it's met).

## Template
```java
// Variable window: longest window satisfying a condition
int l = 0; /* window state */; int best = 0;
for (int r = 0; r < n; r++) {
    // include a[r] into the window state
    while (/* window violates the constraint */) {
        // remove a[l] from the window state
        l++;
    }
    best = Math.max(best, r - l + 1);
}
```

## Problems

### P1. Longest substring without repeating characters (variable)
```java
int lengthOfLongestSubstring(String s) {
    Map<Character,Integer> last = new HashMap<>();  // char -> last index seen
    int l = 0, best = 0;
    for (int r = 0; r < s.length(); r++) {
        char c = s.charAt(r);
        if (last.containsKey(c) && last.get(c) >= l) l = last.get(c) + 1; // jump l past the dup
        last.put(c, r);
        best = Math.max(best, r - l + 1);
    }
    return best;
}
```
**Why:** the window `[l..r]` holds unique chars; when `r` repeats a char still inside the window, jump `l` just past its previous position. **Time O(n), Space O(min(n, charset)).**

### P2. Minimum size subarray sum ≥ target (variable, positives)
```java
int minSubArrayLen(int target, int[] a) {
    int l = 0, sum = 0, best = Integer.MAX_VALUE;
    for (int r = 0; r < a.length; r++) {
        sum += a[r];
        while (sum >= target) {                 // shrink while still valid to find the smallest
            best = Math.min(best, r - l + 1);
            sum -= a[l++];
        }
    }
    return best == Integer.MAX_VALUE ? 0 : best;
}
```
**Why:** with positive numbers, growing right increases the sum and shrinking left decreases it — monotonic, so a window works. **Time O(n)** (each element enters/leaves once), **Space O(1).** *(Note: with negatives this breaks — use prefix sum + deque instead.)*

### P3. Longest substring with at most K distinct characters (variable, counts)
```java
int longestKDistinct(String s, int k) {
    Map<Character,Integer> cnt = new HashMap<>();
    int l = 0, best = 0;
    for (int r = 0; r < s.length(); r++) {
        cnt.merge(s.charAt(r), 1, Integer::sum);
        while (cnt.size() > k) {                 // too many distinct -> shrink
            char cl = s.charAt(l++);
            if (cnt.merge(cl, -1, Integer::sum) == 0) cnt.remove(cl);
        }
        best = Math.max(best, r - l + 1);
    }
    return best;
}
```
**Why:** the map is the window's distinct-char counts; shrink until ≤ K distinct. **Time O(n), Space O(k).** The "at most K" phrasing is a strong sliding-window tell.

### P4. Fixed-size window: max sum of any window of size k
```java
int maxSumK(int[] a, int k) {
    int sum = 0, best;
    for (int i = 0; i < k; i++) sum += a[i];     // first window
    best = sum;
    for (int r = k; r < a.length; r++) {         // slide: add new, drop old
        sum += a[r] - a[r - k];
        best = Math.max(best, sum);
    }
    return best;
}
```
**Time O(n), Space O(1).**

### P5. Find all anagrams of p in s (fixed window + char counts)
```java
List<Integer> findAnagrams(String s, String p) {
    List<Integer> res = new ArrayList<>();
    if (s.length() < p.length()) return res;
    int[] need = new int[26], win = new int[26];
    for (char c : p.toCharArray()) need[c - 'a']++;
    for (int r = 0; r < s.length(); r++) {
        win[s.charAt(r) - 'a']++;
        if (r >= p.length()) win[s.charAt(r - p.length()) - 'a']--; // drop left
        if (r >= p.length() - 1 && Arrays.equals(win, need)) res.add(r - p.length() + 1);
    }
    return res;
}
```
**Time O(n), Space O(1)** (26-size arrays).

## Follow-ups & optimizations
- *"Exactly K distinct?"* → `atMost(K) - atMost(K-1)` (a very common trick).
- *"Window max/min in each window?"* → that's a **monotonic deque** (see pattern 08-adjacent); heap works but is O(n log k).
- *"Array has negatives, sum = K?"* → sliding window fails (non-monotonic) → use **prefix sum + HashMap** (pattern 03).
- Edge cases: empty string, k = 0, window larger than input.

## Complexity summary
Almost always **O(n) time** (each index enters and leaves the window once), **O(1)** or **O(k)** space for the window state.
