# 31 — DSA Patterns (interview-oriented, Java)

> **Priority: P0 for the loop.** DSA is usually 1–3 rounds of an SDE-2 loop. This module is organized by **pattern**, not by data structure, because interviews are won by *recognizing which pattern a problem fits* and reaching for the right template — then adapting it. Every pattern file follows the same shape: how to identify it, the template, representative problems with full Java solutions and explanations, time/space complexity, follow-up questions, and optimizations.

**How to study:** learn the pattern → do its template from memory → solve 3–5 problems → practice *identifying* the pattern from a cold prompt (that recognition is the real interview skill). Then mix patterns so you can tell them apart under pressure.

**Language:** Java (17/21). Solutions favor clarity and the idiomatic interview approach, with the optimization called out explicitly.

---

## The patterns (study order)

| # | Pattern | File | Core idea | Priority |
|---|---|---|---|---|
| 1 | Two Pointers | [two-pointers](./patterns/01-two-pointers.md) | converge/opposite ends or same-direction pointers on sorted/paired data | P0 |
| 2 | Sliding Window | [sliding-window](./patterns/02-sliding-window.md) | contiguous subarray/substring with a moving window | P0 |
| 3 | Prefix Sum | [prefix-sum](./patterns/03-prefix-sum.md) | range queries / subarray sums via cumulative sums | P0 |
| 4 | Fast & Slow Pointers | [fast-slow-pointers](./patterns/04-fast-slow-pointers.md) | cycle detection, middle, happy number | P1 |
| 5 | Cyclic Sort | [cyclic-sort](./patterns/05-cyclic-sort.md) | numbers in range [1..n] → place at index | P1 |
| 6 | Binary Search (+ on answer) | [binary-search](./patterns/06-binary-search.md) | sorted search space OR monotonic predicate on the answer | P0 |
| 7 | Merge Intervals | [merge-intervals](./patterns/07-merge-intervals.md) | overlapping intervals, scheduling | P1 |
| 8 | Monotonic Stack | [monotonic-stack](./patterns/08-monotonic-stack.md) | next/previous greater/smaller element | P1 |
| 9 | Top-K / Heap | [heap-top-k](./patterns/09-heap-top-k.md) | k largest/smallest/frequent, streaming | P0 |
| 10 | Two Heaps | [two-heaps](./patterns/10-two-heaps.md) | running median, balance two halves | P2 |
| 11 | Linked List In-place Reversal | [linked-list](./patterns/11-linked-list.md) | reverse all / sublist / k-group | P1 |
| 12 | Tree BFS / DFS | [tree-traversal](./patterns/12-tree-traversal.md) | level order, path sums, traversals | P0 |
| 13 | Binary Search Tree | [bst](./patterns/13-bst.md) | ordered tree ops, validation, kth | P1 |
| 14 | Graph BFS / DFS | [graph-bfs-dfs](./patterns/14-graph-bfs-dfs.md) | connectivity, shortest unweighted path, islands | P0 |
| 15 | Topological Sort | [topological-sort](./patterns/15-topological-sort.md) | dependency ordering of a DAG | P1 |
| 16 | Union-Find (DSU) | [union-find](./patterns/16-union-find.md) | connected components, cycle in undirected | P1 |
| 17 | Backtracking | [backtracking](./patterns/17-backtracking.md) | subsets/permutations/combinations/constraint search | P0 |
| 18 | Dynamic Programming | [dynamic-programming](./patterns/18-dynamic-programming.md) | overlapping subproblems + optimal substructure | P0 |
| 19 | Greedy | [greedy](./patterns/19-greedy.md) | locally optimal choice → global optimum (prove it) | P1 |
| 20 | Bit Manipulation | [bit-manipulation](./patterns/20-bit-manipulation.md) | XOR tricks, masks, single number | P2 |
| 21 | Trie | [trie](./patterns/21-trie.md) | prefix search, word dictionaries | P2 |

Question bank: [`question-bank.md`](./question-bank.md). Cheat sheet: [`cheatsheet.md`](./cheatsheet.md).

---

## How to identify the pattern from a cold prompt

This table is the single most useful thing here — scan the prompt for these signals.

| If the prompt says / implies… | Reach for | Why |
|---|---|---|
| sorted array, "pair/triplet summing to X", "remove duplicates in place" | **Two Pointers** | one pass, O(1) space on sorted/paired data |
| "contiguous subarray/substring", "longest/shortest window with condition", "at most K" | **Sliding Window** | window grows/shrinks instead of re-scanning |
| "sum of range", "subarray sums equal to K", many range queries | **Prefix Sum** (+ HashMap) | precompute cumulative sums |
| linked list "cycle", "middle", "happy number" | **Fast & Slow Pointers** | two speeds meet inside a cycle |
| array of numbers in range [1..n], "find missing/duplicate" O(1) space | **Cyclic Sort** | place each value at its index |
| sorted input, "search", "first/last position", OR "minimize the max / find smallest X that works" | **Binary Search / on answer** | halve a monotonic search space |
| "overlapping intervals", "merge", "meeting rooms", "insert interval" | **Merge Intervals** | sort by start, sweep |
| "next greater/smaller element", "largest rectangle", "stock span" | **Monotonic Stack** | maintain increasing/decreasing stack |
| "top K", "K most frequent", "K closest", streaming max/min | **Heap (Top-K)** | size-K heap keeps the answer |
| "median of a stream", "balance two halves" | **Two Heaps** | max-heap low half, min-heap high half |
| "reverse the list / a sublist / in groups of k", O(1) space | **Linked-List Reversal** | pointer rewiring |
| tree "level order", "right side view", "zigzag", path sums, LCA | **Tree BFS/DFS** | queue (BFS) or recursion/stack (DFS) |
| "BST", "validate", "kth smallest", "inorder is sorted" | **BST** | inorder = sorted; ordered navigation |
| grid "islands/regions", "shortest steps (unweighted)", "connected", "clone graph" | **Graph BFS/DFS** | explore from sources |
| "course schedule", "build order", "dependencies", DAG ordering | **Topological Sort** | Kahn's (in-degree) or DFS |
| "number of connected components", "redundant connection", dynamic merging | **Union-Find** | near-O(1) union/find |
| "all subsets/permutations/combinations", "N-Queens", "generate all valid…" | **Backtracking** | DFS choose → recurse → undo |
| "min/max/count ways", "can you reach/partition", choices with overlapping subproblems | **Dynamic Programming** | memoize/tabulate states |
| "minimum number of…", "maximum you can take", schedule by end time | **Greedy** | prove the exchange argument |
| "single number", "appears once/twice", "subsets via bits", no extra space | **Bit Manipulation** | XOR / masks |
| "prefix", "autocomplete", "dictionary of words", "startsWith" | **Trie** | character tree |

**Ambiguity resolution:** several patterns can fit one problem (e.g., "subarray sum = K" is prefix-sum; "longest subarray sum ≤ K with positives" is sliding window). The identification table narrows it; the pattern file's §Identify refines it.

---

## Complexity cheat sheet (what interviewers expect you to state)

| Structure / op | Time | Note |
|---|---|---|
| Array access / amortized append | O(1) | |
| Array insert/delete middle | O(n) | shift |
| HashMap/HashSet get/put (avg) | O(1) | O(n) worst-case |
| TreeMap/TreeSet | O(log n) | sorted |
| Binary search | O(log n) | sorted / monotonic |
| Heap push/pop | O(log n) | peek O(1) |
| Build heap | O(n) | heapify |
| Sort (Arrays.sort / Collections.sort) | O(n log n) | |
| BFS/DFS graph | O(V + E) | |
| Union-Find op | ~O(α(n)) ≈ O(1) | with path compression + union by rank |
| DP | O(states × transition) | state the states |

**Always state both time and space**, and distinguish average vs worst case (HashMap), and recursion stack space (O(h) for trees, O(n) worst case for a skewed tree / deep recursion).

---

## Interview delivery (how to use a pattern well)
1. **Clarify** — input size (hints at the target complexity), ranges, duplicates, sorted?, empty/edge inputs, in-place requirement.
2. **State the brute force + its complexity** first — shows you understand the problem and gives a baseline to beat.
3. **Name the pattern and why** — "this is a sliding window because we want the longest contiguous window with a constraint."
4. **Code the template, adapt it, dry-run a small example.**
5. **State complexity and edge cases**, then discuss the follow-up/optimization.

Target complexity from input size: n ≤ 20 → exponential/backtracking OK; n ≤ 2000 → O(n²) OK; n ≤ 10⁵–10⁶ → need O(n log n) or O(n); huge/streaming → O(1) space / online.

---

## Scope note
This is **interview-oriented DSA**: the patterns and the ~120 highest-signal problems, with the reasoning to recognize and adapt them. It is not a substitute for grinding volume on a judge — use it as the map, then practice problems against it. It complements (does not replace) the backend chapters; a complete SDE-2 loop is DSA (this) + LLD + HLD + backend knowledge (ch.01–26) + behavioral (ch.29).
