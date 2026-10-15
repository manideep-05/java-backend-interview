# Pattern 17 — Backtracking

## Identify it when
- You must **generate all** solutions / combinations / arrangements, or **search** a constrained space.
- "All subsets / permutations / combinations", "N-Queens", "Sudoku solver", "word search", "generate parentheses", "palindrome partitioning", "combination sum".
- Input size is **small** (n ≤ ~15–20) because the search is exponential.

**Core idea:** DFS the decision tree — **choose** an option, **recurse**, then **undo** the choice (backtrack). **Prune** branches that can't lead to a valid solution to cut the search.

## Template
```java
void backtrack(State state, List<Result> results) {
    if (isComplete(state)) { results.add(copyOf(state)); return; }
    for (Choice c : choices(state)) {
        if (!valid(c, state)) continue;   // prune
        apply(c, state);                  // choose
        backtrack(state, results);        // recurse
        undo(c, state);                   // backtrack (undo)
    }
}
```

## Problems

### P1. Subsets (the power set)
```java
List<List<Integer>> subsets(int[] a) {
    List<List<Integer>> res = new ArrayList<>();
    dfs(a, 0, new ArrayList<>(), res);
    return res;
}
void dfs(int[] a, int start, List<Integer> cur, List<List<Integer>> res) {
    res.add(new ArrayList<>(cur));                 // every node is a valid subset
    for (int i = start; i < a.length; i++) {
        cur.add(a[i]);                             // choose
        dfs(a, i + 1, cur, res);                   // recurse with the rest
        cur.remove(cur.size() - 1);                // undo
    }
}
```
**Time O(2^n · n)** (2^n subsets, O(n) to copy each), **Space O(n)** recursion. For subsets with duplicates, sort and `if (i > start && a[i]==a[i-1]) continue;`.

### P2. Permutations
```java
List<List<Integer>> permute(int[] a) {
    List<List<Integer>> res = new ArrayList<>();
    dfs(a, new boolean[a.length], new ArrayList<>(), res);
    return res;
}
void dfs(int[] a, boolean[] used, List<Integer> cur, List<List<Integer>> res) {
    if (cur.size() == a.length) { res.add(new ArrayList<>(cur)); return; }
    for (int i = 0; i < a.length; i++) {
        if (used[i]) continue;                     // skip already-chosen
        used[i] = true; cur.add(a[i]);
        dfs(a, used, cur, res);
        cur.remove(cur.size() - 1); used[i] = false; // undo both
    }
}
```
**Time O(n! · n), Space O(n).**

### P3. Combination sum (reuse allowed)
```java
List<List<Integer>> combinationSum(int[] cand, int target) {
    List<List<Integer>> res = new ArrayList<>();
    dfs(cand, 0, target, new ArrayList<>(), res);
    return res;
}
void dfs(int[] c, int start, int rem, List<Integer> cur, List<List<Integer>> res) {
    if (rem == 0) { res.add(new ArrayList<>(cur)); return; }
    if (rem < 0) return;                           // prune: overshoot
    for (int i = start; i < c.length; i++) {
        cur.add(c[i]);
        dfs(c, i, rem - c[i], cur, res);           // i (not i+1) -> reuse the same number
        cur.remove(cur.size() - 1);
    }
}
```
**Why:** `rem < 0` pruning cuts dead branches early. **Time O(exponential), Space O(target/min).**

### P4. N-Queens (constraint search with pruning)
```java
List<List<String>> solveNQueens(int n) {
    List<List<String>> res = new ArrayList<>();
    place(0, n, new int[n], new boolean[n], new boolean[2*n], new boolean[2*n], res);
    return res;
}
void place(int row, int n, int[] cols, boolean[] col, boolean[] diag, boolean[] anti,
           List<List<String>> res) {
    if (row == n) { res.add(build(cols, n)); return; }
    for (int c = 0; c < n; c++) {
        int d = row - c + n, a = row + c;
        if (col[c] || diag[d] || anti[a]) continue;   // prune attacked squares
        col[c] = diag[d] = anti[a] = true; cols[row] = c;
        place(row + 1, n, cols, col, diag, anti, res);
        col[c] = diag[d] = anti[a] = false;           // undo
    }
}
```
**Why:** boolean arrays for column/diagonal occupancy give O(1) validity checks — pruning is what makes it tractable. **Time O(n!) worst, far less with pruning; Space O(n).**

### P5. Word search in a grid (DFS backtracking)
```java
boolean exist(char[][] board, String word) {
    for (int i = 0; i < board.length; i++)
        for (int j = 0; j < board[0].length; j++)
            if (dfs(board, i, j, word, 0)) return true;
    return false;
}
boolean dfs(char[][] b, int i, int j, String w, int k) {
    if (k == w.length()) return true;
    if (i<0||j<0||i>=b.length||j>=b[0].length||b[i][j]!=w.charAt(k)) return false;
    char tmp = b[i][j]; b[i][j] = '#';                 // mark visited
    boolean found = dfs(b,i+1,j,w,k+1)||dfs(b,i-1,j,w,k+1)||dfs(b,i,j+1,w,k+1)||dfs(b,i,j-1,w,k+1);
    b[i][j] = tmp;                                     // restore (backtrack)
    return found;
}
```
**Time O(R·C·4^L), Space O(L)** (L = word length).

## Follow-ups & optimizations
- *Pruning is everything* — eliminating invalid branches early (overshoot in combination sum, attacked squares in N-Queens) is what makes exponential search feasible.
- *Subsets/combinations use a `start` index* to avoid duplicate orderings; permutations use a `used[]` flag.
- *Duplicates in input* → sort + skip equal siblings (`i > start && a[i]==a[i-1]`).
- *"Count solutions, not list them?"* → often convertible to DP (e.g., combination *count* = coin change DP, pattern 18) — a strong "I'd switch patterns" answer.

## Complexity summary
Inherently exponential: subsets **O(2^n)**, permutations **O(n!)**, with **O(n)/O(depth)** recursion space. Viable only for small n; pruning reduces the constant/branches dramatically. If the problem asks for a *count* or *optimum* (not all solutions), consider DP instead.
