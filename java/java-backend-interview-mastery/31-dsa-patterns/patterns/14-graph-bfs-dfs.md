# Pattern 14 — Graph BFS / DFS

## Identify it when
- The input is a **graph or grid** and you need **connectivity**, **reachability**, **number of components/islands**, or the **shortest path in an *unweighted* graph**.
- Grid problems ("number of islands", "flood fill", "rotting oranges", "walls and gates", "shortest path in a binary matrix") are implicit graphs (cells = nodes, adjacency = up/down/left/right).
- **BFS** for **shortest unweighted path / level-by-level spread**; **DFS** for **reachability / components / cycle detection / flood fill**.

**Core idea:** explore from source(s), marking visited to avoid revisiting. Time **O(V + E)** (every node and edge once).

## Templates
```java
// BFS from a source (shortest steps in an unweighted graph)
Queue<Integer> q = new LinkedList<>(); boolean[] vis = new boolean[n];
q.offer(src); vis[src] = true; int dist = 0;
while (!q.isEmpty()) {
    int sz = q.size();
    for (int i = 0; i < sz; i++) {
        int u = q.poll();
        for (int v : adj[u]) if (!vis[v]) { vis[v] = true; q.offer(v); }
    }
    dist++;
}
// DFS (recursive)
void dfs(int u, boolean[] vis, List<Integer>[] adj) {
    vis[u] = true;
    for (int v : adj[u]) if (!vis[v]) dfs(v, vis, adj);
}
```

## Problems

### P1. Number of islands (grid DFS flood fill)
```java
int numIslands(char[][] g) {
    int count = 0;
    for (int i = 0; i < g.length; i++)
        for (int j = 0; j < g[0].length; j++)
            if (g[i][j] == '1') { count++; sink(g, i, j); }   // found a new island -> sink it
    return count;
}
void sink(char[][] g, int i, int j) {
    if (i < 0 || j < 0 || i >= g.length || j >= g[0].length || g[i][j] != '1') return;
    g[i][j] = '0';                                            // mark visited (in place)
    sink(g, i+1, j); sink(g, i-1, j); sink(g, i, j+1); sink(g, i, j-1);
}
```
**Why:** each unvisited land cell starts a new island; DFS sinks the whole connected region. **Time O(R·C), Space O(R·C)** worst-case recursion. (BFS avoids deep recursion on huge grids.)

### P2. Rotting oranges (multi-source BFS, shortest time)
```java
int orangesRotting(int[][] g) {
    int R = g.length, C = g[0].length, fresh = 0, minutes = 0;
    Queue<int[]> q = new LinkedList<>();
    for (int i = 0; i < R; i++) for (int j = 0; j < C; j++) {
        if (g[i][j] == 2) q.offer(new int[]{i, j});           // all rotten = sources
        else if (g[i][j] == 1) fresh++;
    }
    int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
    while (!q.isEmpty() && fresh > 0) {
        minutes++;
        for (int sz = q.size(); sz > 0; sz--) {
            int[] c = q.poll();
            for (int[] d : dirs) {
                int ni = c[0]+d[0], nj = c[1]+d[1];
                if (ni>=0 && nj>=0 && ni<R && nj<C && g[ni][nj]==1) {
                    g[ni][nj] = 2; fresh--; q.offer(new int[]{ni, nj});
                }
            }
        }
    }
    return fresh == 0 ? minutes : -1;
}
```
**Why:** **multi-source BFS** spreads from all rotten oranges simultaneously; levels = minutes. **Time O(R·C), Space O(R·C).** Multi-source BFS is the key trick for "shortest time to fill/reach from many starts" (also "walls and gates", "01 matrix").

### P3. Clone graph (DFS/BFS with a visited map)
```java
Map<Node,Node> seen = new HashMap<>();
Node cloneGraph(Node node) {
    if (node == null) return null;
    if (seen.containsKey(node)) return seen.get(node);        // already cloned -> reuse
    Node copy = new Node(node.val);
    seen.put(node, copy);                                     // register BEFORE recursing (cycles)
    for (Node nb : node.neighbors) copy.neighbors.add(cloneGraph(nb));
    return copy;
}
```
**Why:** register the clone before recursing so cycles terminate. **Time O(V + E), Space O(V).**

### P4. Shortest path in a binary matrix (BFS, 8-directional)
```java
int shortestPathBinaryMatrix(int[][] g) {
    int n = g.length;
    if (g[0][0] == 1 || g[n-1][n-1] == 1) return -1;
    Queue<int[]> q = new LinkedList<>(); q.offer(new int[]{0,0});
    boolean[][] vis = new boolean[n][n]; vis[0][0] = true;
    int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};
    int dist = 1;
    while (!q.isEmpty()) {
        for (int sz = q.size(); sz > 0; sz--) {
            int[] c = q.poll();
            if (c[0]==n-1 && c[1]==n-1) return dist;
            for (int[] d : dirs) {
                int ni=c[0]+d[0], nj=c[1]+d[1];
                if (ni>=0&&nj>=0&&ni<n&&nj<n&&!vis[ni][nj]&&g[ni][nj]==0){ vis[ni][nj]=true; q.offer(new int[]{ni,nj}); }
            }
        }
        dist++;
    }
    return -1;
}
```
**Why:** BFS gives the shortest number of steps in an unweighted graph. **Time O(n²), Space O(n²).**

### P5. Detect a cycle in a directed graph (DFS with 3 colors)
```java
boolean hasCycle(int n, List<Integer>[] adj) {
    int[] state = new int[n];                      // 0=unvisited, 1=in-stack, 2=done
    for (int i = 0; i < n; i++) if (state[i] == 0 && dfs(i, adj, state)) return true;
    return false;
}
boolean dfs(int u, List<Integer>[] adj, int[] state) {
    state[u] = 1;                                  // on the current DFS path
    for (int v : adj[u]) {
        if (state[v] == 1) return true;            // back edge -> cycle
        if (state[v] == 0 && dfs(v, adj, state)) return true;
    }
    state[u] = 2;                                  // fully explored
    return false;
}
```
**Time O(V + E), Space O(V).** (Undirected cycle detection → union-find, pattern 16.)

## Follow-ups & optimizations
- *Weighted shortest path?* → BFS no longer works; use **Dijkstra** (min-heap, non-negative weights) or **Bellman-Ford** (handles negatives). Mention this when weights appear.
- *BFS vs DFS?* → BFS for shortest unweighted path and level spread; DFS for components/reachability/cycle. State which and why.
- *Recursion depth on huge grids* → convert DFS to iterative BFS/stack to avoid stack overflow.
- *Visited marking* → mark **on enqueue** (BFS) to avoid adding a node twice; for grids you can mutate the grid in place to save the visited array.

## Complexity summary
BFS/DFS: **O(V + E) time, O(V) space** (grids: O(R·C)). Multi-source BFS is the pattern for "shortest time from many starts". Weighted → Dijkstra O((V+E) log V).
