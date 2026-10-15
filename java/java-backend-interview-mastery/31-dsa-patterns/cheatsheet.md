# DSA One-Page Cheat Sheet

Night-before / pre-round sweep. Pattern → the signal that triggers it → the move → complexity.

## Identify-the-pattern (scan the prompt)
| Signal in the prompt | Pattern | Move | Typical complexity |
|---|---|---|---|
| sorted, pair/triplet sum, in-place dedup | **Two Pointers** | converge from ends / slow-fast | O(n) / O(n²) for 3Sum |
| contiguous subarray/substring, longest/shortest/at-most-K | **Sliding Window** | grow right, shrink left | O(n) |
| subarray sum = K, range sums, has negatives | **Prefix Sum + HashMap** | prefix[j]-prefix[i] | O(n) |
| linked list cycle / middle / nth-from-end | **Fast & Slow** | 2x vs 1x pointer | O(n), O(1) space |
| n numbers in [1..n], find missing/dup, O(1) space | **Cyclic Sort** | place value at index v-1 | O(n), O(1) |
| sorted search OR "min the max / smallest X that works" | **Binary Search (on answer)** | halve monotonic space | O(log n) / O(n log range) |
| overlapping intervals, meetings, merge/insert | **Merge Intervals** | sort by start/end, sweep | O(n log n) |
| next/prev greater/smaller, histogram, window max | **Monotonic Stack/Deque** | keep stack sorted, pop resolves | O(n) |
| top K / K frequent / K closest / stream | **Heap (size-K)** | min-heap for K largest | O(n log k) |
| median of a stream, balance halves | **Two Heaps** | max-heap low, min-heap high | add O(log n), read O(1) |
| reverse list / sublist / k-group, O(1) space | **Linked-List Reversal** | prev/cur/next rewire + dummy | O(n), O(1) |
| tree level-order / right view / zigzag / min depth | **Tree BFS** | queue, snapshot level size | O(n) |
| tree path/subtree/depth/diameter/LCA | **Tree DFS** | recurse, return + global | O(n), O(h) |
| BST, validate, kth, inorder=sorted | **BST** | ordered navigation / inorder | O(h) / O(n) |
| grid islands/regions, shortest unweighted, connected | **Graph BFS/DFS** | explore from sources | O(V+E) |
| shortest *time* from many starts | **Multi-source BFS** | enqueue all sources | O(V+E) |
| dependencies / build order / DAG | **Topological Sort** | Kahn's in-degree / DFS | O(V+E) |
| dynamic merging, # components, redundant edge, undirected cycle | **Union-Find** | find+union, path compression | ~O(α) ≈ O(1) |
| all subsets/perms/combos, N-Queens, constraint search | **Backtracking** | choose→recurse→undo + prune | exponential |
| min/max/count-ways, reach/partition, overlapping subproblems | **Dynamic Programming** | define state + recurrence | states × transition |
| "minimum/maximum …", schedule by end time | **Greedy** | sort + local choice (prove it) | O(n log n) |
| appears once/odd, O(1) space counting, subsets of small set | **Bit Manipulation** | XOR / masks | O(n) / O(2^n) |
| prefix / autocomplete / dictionary / startsWith | **Trie** | char tree, isWord flag | O(L) per op |

## Target complexity from input size
- n ≤ 12 → O(n!) permutations OK · n ≤ 20 → O(2^n) subsets/bitmask OK
- n ≤ 500 → O(n³) · n ≤ 5000 → O(n²) · n ≤ 10⁵–10⁶ → O(n log n) or O(n) · n > 10⁶ / streaming → O(n), O(1) space

## Core complexities to recite
array O(1) access · HashMap O(1) avg (O(n) worst) · TreeMap/heap O(log n) · sort O(n log n) · BFS/DFS O(V+E) · union-find ~O(1) · DP = states×transition. Always state **time AND space** (and recursion-stack space O(h)).

## Must-know code idioms
- Overflow-safe mid: `lo + (hi - lo) / 2`
- Lower bound (first true): half-open `[lo, hi)`, `if (pred(mid)) hi = mid; else lo = mid + 1;`
- Clear lowest set bit: `x & (x - 1)` · isolate it: `x & -x`
- Union-Find: path compression + union by rank → near O(1)
- Dummy head for linked-list edits that may change the head
- Snapshot `q.size()` to process a BFS level
- Backtracking: `choose → recurse → undo`
- 0/1 knapsack: iterate capacity **downward**; unbounded: **upward**

## Interview flow (say it every time)
clarify (size/range/dupes/sorted/edge) → brute force + its O() → **name the pattern and why** → code the template, adapt → dry-run small input → state time & space → discuss the follow-up/optimization.

## When two patterns compete
- sum=K with negatives → **prefix sum**, not sliding window
- "all solutions" → **backtracking**; "count/optimum of them" → **DP**
- greedy *looks* right but you can't prove the exchange argument → suspect **DP**
- find duplicate, O(1) space, don't modify array → **Floyd (fast/slow)**, not cyclic sort
- undirected cycle → **union-find**; directed cycle → **DFS 3-color / topo sort**
