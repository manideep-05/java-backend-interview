# Pattern 16 — Union-Find (Disjoint Set Union)

## Identify it when
- You need to track **connected components** that **merge dynamically**, or answer "are A and B connected?" repeatedly.
- "Number of connected components", "redundant connection", "accounts merge", "number of provinces", "detect cycle in an *undirected* graph", "friend circles", "Kruskal's MST".
- The graph **grows by unions** over time (online) — union-find shines where BFS/DFS would need re-running.

**Core idea:** each element points to a parent; the root identifies the set. **find** returns the root (with **path compression**), **union** merges two roots (**by rank/size**). With both optimizations, operations are near **O(1)** amortized (inverse-Ackermann α).

## Template
```java
class DSU {
    int[] parent, rank; int count;             // count = number of components
    DSU(int n) { parent = new int[n]; rank = new int[n]; count = n;
        for (int i = 0; i < n; i++) parent[i] = i; }
    int find(int x) {                           // path compression
        while (parent[x] != x) { parent[x] = parent[parent[x]]; x = parent[x]; }
        return x;
    }
    boolean union(int a, int b) {               // union by rank; returns false if already joined
        int ra = find(a), rb = find(b);
        if (ra == rb) return false;             // same set already -> would form a cycle
        if (rank[ra] < rank[rb]) { int t = ra; ra = rb; rb = t; }
        parent[rb] = ra;
        if (rank[ra] == rank[rb]) rank[ra]++;
        count--;                                // two components became one
        return true;
    }
}
```

## Problems

### P1. Number of connected components in an undirected graph
```java
int countComponents(int n, int[][] edges) {
    DSU dsu = new DSU(n);
    for (int[] e : edges) dsu.union(e[0], e[1]);
    return dsu.count;                           // starts at n, decremented on each real union
}
```
**Why:** every successful union merges two components. **Time O(E · α(n)) ≈ O(E), Space O(n).**

### P2. Redundant connection (find the edge that creates a cycle)
```java
int[] findRedundantConnection(int[][] edges) {
    DSU dsu = new DSU(edges.length + 1);
    for (int[] e : edges)
        if (!dsu.union(e[0], e[1])) return e;   // union fails => both ends already connected => cycle edge
    return new int[0];
}
```
**Why:** processing edges in order, the first edge whose endpoints are already in the same set closes a cycle. **Time O(E · α), Space O(n).** This is the canonical **undirected cycle detection** (contrast with the directed 3-color DFS in pattern 14).

### P3. Number of provinces (adjacency matrix → components)
```java
int findCircleNum(int[][] isConnected) {
    int n = isConnected.length;
    DSU dsu = new DSU(n);
    for (int i = 0; i < n; i++)
        for (int j = i + 1; j < n; j++)
            if (isConnected[i][j] == 1) dsu.union(i, j);
    return dsu.count;
}
```
**Time O(n² · α), Space O(n).**

### P4. Accounts merge (union by owner, group emails)
```java
// Sketch: map each email to an id; union emails that appear in the same account;
// then group emails by their DSU root and attach the owner name.
// Time O(N·K·α) over N accounts of K emails; Space O(total emails).
```
**Why:** emails in one account belong to one person; transitive sharing merges accounts — exactly dynamic connectivity. Union-find groups them in near-linear time where a graph traversal would be clumsier.

### P5. Kruskal's MST (union-find as the backbone)
```java
int kruskalMST(int n, int[][] edges) {          // edges: {u, v, weight}
    Arrays.sort(edges, (a, b) -> Integer.compare(a[2], b[2])); // cheapest first
    DSU dsu = new DSU(n);
    int cost = 0, used = 0;
    for (int[] e : edges) {
        if (dsu.union(e[0], e[1])) { cost += e[2]; if (++used == n - 1) break; } // add if no cycle
    }
    return cost;
}
```
**Why:** greedily add the cheapest edge that doesn't form a cycle (detected by union-find). **Time O(E log E) (sort), Space O(n).**

## Follow-ups & optimizations
- *"Why not BFS/DFS for components?"* → fine for a static graph (O(V+E)); union-find wins when edges arrive **dynamically/online** or you do many connectivity queries interleaved with unions.
- *Both optimizations matter:* path compression + union by rank/size give α(n) ≈ O(1); without them, find can degrade to O(n).
- *"Count largest component / component sizes?"* → track a `size[]` array updated on union.
- *Directed cycle?* → union-find is for **undirected**; use DFS 3-coloring or Kahn's for directed (patterns 14/15).

## Complexity summary
With path compression + union by rank: each `find`/`union` is **~O(α(n)) ≈ O(1)** amortized; overall **O(E · α(n)) ≈ O(E)** time, **O(n)** space. The identifying signal is **dynamic merging / repeated connectivity queries**.
