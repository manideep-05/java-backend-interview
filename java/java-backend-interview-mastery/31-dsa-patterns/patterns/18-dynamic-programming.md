# Pattern 18 — Dynamic Programming

> The highest-leverage and most-feared pattern. The skill isn't memorizing solutions — it's **recognizing a DP problem and defining the state**. This file goes deeper than the others.

## Identify it when
- The problem asks for a **count of ways**, a **min/max value**, or **can you reach/partition/make** something, AND
- The choices create **overlapping subproblems** (the same sub-input recurs) with **optimal substructure** (the optimal answer is built from optimal sub-answers).
- Keywords: "min/max cost/path", "number of ways", "longest/shortest …", "can you partition/reach", "best you can do".
- A backtracking/recursive solution exists but **recomputes the same states** → add memoization → DP.

**The DP recipe (say this out loud in an interview):**
1. **Define the state** — what parameters uniquely describe a subproblem? (e.g., `dp[i]` = answer considering the first `i` items).
2. **Transition** — how does a state depend on smaller states? (the recurrence).
3. **Base cases.**
4. **Order** — memoized recursion (top-down) or tabulation (bottom-up).
5. **Answer** — which state holds it.
6. **Optimize space** — can you keep only the last row/few variables?

## Problems — grouped by DP sub-type

### 1D DP

#### P1. Climbing stairs / Fibonacci (intro to the idea)
```java
int climbStairs(int n) {
    int a = 1, b = 1;                   // ways to reach step i-2 and i-1
    for (int i = 2; i <= n; i++) { int c = a + b; a = b; b = c; }
    return b;
}
```
**State:** `dp[i]` = ways to reach step i; **transition:** `dp[i] = dp[i-1] + dp[i-2]`. **Time O(n), Space O(1)** (rolling two variables).

#### P2. House robber (choice: take or skip)
```java
int rob(int[] a) {
    int prev2 = 0, prev1 = 0;           // best up to i-2 and i-1
    for (int x : a) {
        int cur = Math.max(prev1, prev2 + x);  // skip i, or rob i + best up to i-2
        prev2 = prev1; prev1 = cur;
    }
    return prev1;
}
```
**State:** `dp[i]` = max loot from houses `0..i`; **transition:** `max(dp[i-1], dp[i-2] + a[i])`. **Time O(n), Space O(1).**

### Unbounded / knapsack-style

#### P3. Coin change (min coins to make amount — unbounded)
```java
int coinChange(int[] coins, int amount) {
    int[] dp = new int[amount + 1];
    Arrays.fill(dp, amount + 1);                 // "infinity"
    dp[0] = 0;
    for (int a = 1; a <= amount; a++)
        for (int c : coins)
            if (c <= a) dp[a] = Math.min(dp[a], dp[a - c] + 1);
    return dp[amount] > amount ? -1 : dp[amount];
}
```
**State:** `dp[a]` = min coins to make `a`; **transition:** `min over coins c of dp[a-c] + 1`. **Time O(amount · #coins), Space O(amount).** (For *number of ways*, loop coins outer, amounts inner, and sum instead of min.)

#### P4. 0/1 Knapsack (each item once)
```java
int knapsack(int[] w, int[] val, int W) {
    int[] dp = new int[W + 1];                   // dp[c] = best value with capacity c
    for (int i = 0; i < w.length; i++)
        for (int c = W; c >= w[i]; c--)          // iterate capacity DOWNWARD for 0/1
            dp[c] = Math.max(dp[c], dp[c - w[i]] + val[i]);
    return dp[W];
}
```
**Why downward:** iterating capacity from high to low ensures each item is used **at most once** (upward would allow reuse = unbounded knapsack). **Time O(n·W), Space O(W).** Variant: "partition equal subset sum" = can we hit sum/2 (boolean knapsack).

### Sequence DP

#### P5. Longest increasing subsequence (LIS)
```java
int lengthOfLIS(int[] a) {
    List<Integer> tails = new ArrayList<>();      // tails[k] = smallest tail of an LIS of length k+1
    for (int x : a) {
        int i = Collections.binarySearch(tails, x);
        if (i < 0) i = -(i + 1);                  // insertion point
        if (i == tails.size()) tails.add(x); else tails.set(i, x);
    }
    return tails.size();
}
```
**Two approaches:** O(n²) DP (`dp[i]` = LIS ending at i, `dp[i] = max(dp[j]+1)` for `a[j] < a[i]`), or the **O(n log n)** patience-sorting version above. Know both; lead with the n² DP for clarity, then optimize. **Time O(n log n), Space O(n).**

#### P6. Longest common subsequence (LCS) — 2D DP
```java
int longestCommonSubsequence(String s, String t) {
    int m = s.length(), n = t.length();
    int[][] dp = new int[m + 1][n + 1];           // dp[i][j] = LCS of s[0..i), t[0..j)
    for (int i = 1; i <= m; i++)
        for (int j = 1; j <= n; j++)
            dp[i][j] = (s.charAt(i-1) == t.charAt(j-1))
                ? dp[i-1][j-1] + 1                 // chars match -> extend
                : Math.max(dp[i-1][j], dp[i][j-1]); // else drop one char from either
    return dp[m][n];
}
```
**State:** `dp[i][j]`; **transition** as above. **Time O(m·n), Space O(m·n)** (reducible to O(min(m,n)) with two rows). Edit distance and many string DPs share this grid shape.

### 2D grid DP

#### P7. Unique paths / minimum path sum (grid)
```java
int minPathSum(int[][] g) {
    int R = g.length, C = g[0].length;
    int[] dp = new int[C];
    dp[0] = g[0][0];
    for (int j = 1; j < C; j++) dp[j] = dp[j-1] + g[0][j];  // first row
    for (int i = 1; i < R; i++) {
        dp[0] += g[i][0];
        for (int j = 1; j < C; j++)
            dp[j] = Math.min(dp[j], dp[j-1]) + g[i][j];      // from top or left
    }
    return dp[C-1];
}
```
**State:** `dp[i][j]` = min cost to reach `(i,j)`; **transition:** `min(up, left) + cost`. **Time O(R·C), Space O(C)** (one row).

## Top-down (memoization) vs bottom-up (tabulation)
```java
// Top-down: write the recursion, add a memo. Easiest to derive from brute force.
Integer[] memo;
int solve(int i) {
    if (i == base) return baseValue;
    if (memo[i] != null) return memo[i];
    return memo[i] = /* recurrence using solve(smaller) */;
}
```
- **Top-down**: natural to derive (brute force + cache), only computes needed states, but recursion stack + hashmap/array overhead.
- **Bottom-up**: no recursion, easy to space-optimize (rolling arrays), but you must order states correctly and may compute unused ones.
- Convert one to the other on request — interviewers often ask.

## Follow-ups & optimizations
- *Space optimization:* most 2D DPs that only read the previous row/column collapse to **O(n)** or **O(1)** — a frequent "can you do better on space?" follow-up (house robber O(1), LCS two rows, knapsack one row).
- *"How did you know it's DP?"* → "a recursive solution recomputes overlapping subproblems and the problem has optimal substructure, so I memoize the state."
- *Reconstruct the solution* (not just its value) → store choices / backtrack through the dp table.
- *0/1 vs unbounded knapsack:* capacity loop **downward** (0/1, each item once) vs **upward** (unbounded, reuse) — a classic gotcha.
- *Count-of-ways vs min/max:* same states, different combine (sum vs min/max) and sometimes loop order (coin change ways loops coins outside to avoid counting permutations).

## Complexity summary
**Time = (number of states) × (work per transition); Space = number of states (often reducible).** Stating the state and the recurrence clearly is 80% of the score — the code follows mechanically. If asked for *all* solutions rather than a count/optimum, it's backtracking (pattern 17), not DP.
