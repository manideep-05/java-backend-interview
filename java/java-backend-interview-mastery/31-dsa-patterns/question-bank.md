# DSA Question Bank — curated must-solve, grouped by pattern

A focused list of the highest-signal problems per pattern. **Solve them in pattern order**, and after each, practice stating: brute force → pattern → optimized complexity → one follow-up. Difficulty: 🟢 easy · 🟡 medium · 🔴 hard. Priority: **P0** core, P1 important, P2 stretch.

> This is a *map*, not a full grind. ~110 problems — enough to internalize every pattern and its variants. Then do volume on a judge to build speed. Problems are named generically (standard interview problems); the pattern file linked shows the technique.

---

## 1. Two Pointers → [01](./patterns/01-two-pointers.md)
- 🟢 Two Sum II (sorted) — **P0**
- 🟢 Valid palindrome — P1
- 🟡 3Sum — **P0**
- 🟡 Container with most water — **P0**
- 🟡 Sort colors (Dutch national flag) — P1
- 🟢 Remove duplicates from sorted array — P1
- 🟡 Trapping rain water (two-pointer version) — 🔴 P1

## 2. Sliding Window → [02](./patterns/02-sliding-window.md)
- 🟡 Longest substring without repeating characters — **P0**
- 🟡 Minimum size subarray sum — P1
- 🟡 Longest substring with at most K distinct — P1
- 🔴 Minimum window substring — **P0**
- 🟡 Find all anagrams in a string — P1
- 🟡 Permutation in string — P1
- 🟡 Longest repeating character replacement — P1

## 3. Prefix Sum → [03](./patterns/03-prefix-sum.md)
- 🟡 Subarray sum equals K — **P0**
- 🟢 Range sum query (immutable) — P1
- 🟡 Contiguous array (equal 0s/1s) — P1
- 🟡 Subarray sums divisible by K — P1
- 🟡 Product of array except self — P1 (prefix/suffix products)
- 🟡 Range sum query 2D (immutable) — P2

## 4. Fast & Slow Pointers → [04](./patterns/04-fast-slow-pointers.md)
- 🟢 Linked list cycle — **P0**
- 🟡 Linked list cycle II (find start) — P1
- 🟢 Middle of the linked list — P1
- 🟢 Happy number — P1
- 🟡 Find the duplicate number — **P0** (Floyd on array)
- 🟡 Remove nth node from end — P1

## 5. Cyclic Sort → [05](./patterns/05-cyclic-sort.md)
- 🟢 Missing number — P1
- 🟢 Find all numbers disappeared — P1
- 🟡 Find the duplicate / set mismatch — P1
- 🔴 First missing positive — **P0**

## 6. Binary Search → [06](./patterns/06-binary-search.md)
- 🟢 Binary search / search insert position — **P0**
- 🟡 First and last position of element — P1
- 🟡 Search in rotated sorted array — **P0**
- 🟡 Find minimum in rotated sorted array — P1
- 🟡 Koko eating bananas (BS on answer) — **P0**
- 🟡 Capacity to ship within D days (BS on answer) — P1
- 🔴 Split array largest sum (BS on answer) — P1
- 🔴 Median of two sorted arrays — P2

## 7. Merge Intervals → [07](./patterns/07-merge-intervals.md)
- 🟡 Merge intervals — **P0**
- 🟡 Insert interval — P1
- 🟢 Meeting rooms I — P1
- 🟡 Meeting rooms II (min rooms) — **P0**
- 🟡 Non-overlapping intervals — P1
- 🟡 Minimum arrows to burst balloons — P1

## 8. Monotonic Stack → [08](./patterns/08-monotonic-stack.md)
- 🟡 Daily temperatures — **P0**
- 🟡 Next greater element I/II — P1
- 🔴 Largest rectangle in histogram — **P0**
- 🔴 Trapping rain water (stack) — P1
- 🔴 Sliding window maximum (deque) — **P0**
- 🟡 Remove K digits — P1

## 9. Heap / Top-K → [09](./patterns/09-heap-top-k.md)
- 🟡 Kth largest element — **P0**
- 🟡 Top K frequent elements — **P0**
- 🟡 K closest points to origin — P1
- 🔴 Merge K sorted lists — **P0**
- 🟢 Kth largest in a stream — P1
- 🔴 Task scheduler (heap variant) — P1

## 10. Two Heaps → [10](./patterns/10-two-heaps.md)
- 🔴 Find median from data stream — **P0**
- 🔴 Sliding window median — P2
- 🔴 IPO (maximize capital) — P2

## 11. Linked List Reversal → [11](./patterns/11-linked-list.md)
- 🟢 Reverse linked list — **P0**
- 🟡 Reverse linked list II (sublist) — P1
- 🔴 Reverse nodes in k-group — **P0**
- 🟢 Palindrome linked list — P1
- 🟡 Reorder list — P1
- 🟡 Add two numbers — P1

## 12. Tree BFS/DFS → [12](./patterns/12-tree-traversal.md)
- 🟡 Binary tree level order — **P0**
- 🟡 Right side view / zigzag — P1
- 🟢 Maximum depth — P1
- 🟡 Path sum II — P1
- 🟡 Diameter of binary tree — P1
- 🔴 Binary tree maximum path sum — **P0**
- 🟡 Lowest common ancestor (binary tree) — **P0**
- 🔴 Serialize and deserialize binary tree — P1

## 13. BST → [13](./patterns/13-bst.md)
- 🟡 Validate BST — **P0**
- 🟡 Kth smallest in BST — P1
- 🟢 Insert / search in BST — P1
- 🟢 LCA of a BST — P1
- 🟢 Convert sorted array to BST — P1

## 14. Graph BFS/DFS → [14](./patterns/14-graph-bfs-dfs.md)
- 🟡 Number of islands — **P0**
- 🟡 Rotting oranges (multi-source BFS) — **P0**
- 🟡 Clone graph — P1
- 🟡 Shortest path in binary matrix — P1
- 🟡 Pacific Atlantic water flow — P1
- 🟡 Surrounded regions — P1
- 🔴 Word ladder (BFS) — P1

## 15. Topological Sort → [15](./patterns/15-topological-sort.md)
- 🟡 Course schedule (can finish) — **P0**
- 🟡 Course schedule II (order) — **P0**
- 🔴 Alien dictionary — P1
- 🟡 Minimum height trees — P2

## 16. Union-Find → [16](./patterns/16-union-find.md)
- 🟡 Number of connected components — **P0**
- 🟡 Redundant connection — P1
- 🟡 Number of provinces — P1
- 🔴 Accounts merge — P1
- 🟡 Graph valid tree — P1

## 17. Backtracking → [17](./patterns/17-backtracking.md)
- 🟡 Subsets / Subsets II — **P0**
- 🟡 Permutations / Permutations II — **P0**
- 🟡 Combination sum I/II — **P0**
- 🟡 Generate parentheses — P1
- 🟡 Word search — P1
- 🔴 N-Queens — P1
- 🟡 Palindrome partitioning — P1
- 🔴 Sudoku solver — P2

## 18. Dynamic Programming → [18](./patterns/18-dynamic-programming.md)
- 🟢 Climbing stairs — **P0** (intro)
- 🟡 House robber I/II — **P0**
- 🟡 Coin change (min) / coin change II (ways) — **P0**
- 🟡 Longest increasing subsequence — **P0**
- 🟡 Longest common subsequence — **P0**
- 🟡 Partition equal subset sum (0/1 knapsack) — P1
- 🟡 Unique paths / minimum path sum — P1
- 🔴 Edit distance — **P0**
- 🔴 Word break — P1
- 🔴 Decode ways — P1
- 🔴 Best time to buy/sell stock with cooldown — P1
- 🔴 Longest palindromic substring — P1

## 19. Greedy → [19](./patterns/19-greedy.md)
- 🟡 Jump game I/II — **P0**
- 🟡 Gas station — P1
- 🟡 Non-overlapping intervals — P1 (overlaps with intervals)
- 🟡 Task scheduler — P1
- 🟢 Assign cookies / lemonade change — P2

## 20. Bit Manipulation → [20](./patterns/20-bit-manipulation.md)
- 🟢 Single number — **P0**
- 🟡 Single number II (thrice) — P1
- 🟢 Number of 1 bits / counting bits — P1
- 🟢 Missing number (XOR) — P1
- 🟢 Power of two — P2
- 🟡 Sum of two integers (no +) — P2

## 21. Trie → [21](./patterns/21-trie.md)
- 🟡 Implement trie — **P0**
- 🔴 Add and search word (`.` wildcard) — P1
- 🔴 Word search II (trie + backtracking) — P1
- 🟡 Replace words — P2
- 🔴 Maximum XOR of two numbers (bitwise trie) — P2

---

## Suggested solving order (3 waves)
1. **Wave 1 — all P0 (~45 problems):** these cover every pattern's core. Finish before interviewing.
2. **Wave 2 — P1:** variants and harder applications; builds adaptability.
3. **Wave 3 — P2:** stretch/hard; do if time allows or the target bar is high.

For each problem, after solving, write one line: *"Signal that told me the pattern + the complexity + one follow-up."* That meta-note is what makes cold recognition automatic.
