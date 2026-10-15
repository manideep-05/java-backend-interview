# Pattern 15 — Topological Sort

## Identify it when
- You have **dependencies / ordering constraints** modeled as a **directed graph**, and you need a valid order (or to detect that none exists because of a cycle).
- "Course schedule", "build order", "task scheduling with prerequisites", "alien dictionary", "find order of X given constraints".
- Only works on a **DAG** (directed acyclic graph); a cycle means no valid ordering.

**Two approaches:**
- **Kahn's algorithm (BFS, in-degree):** repeatedly remove nodes with in-degree 0. If you can't remove all nodes, there's a cycle. (Easier to also detect cycles.)
- **DFS + stack:** post-order DFS, push nodes as they finish, reverse at the end.

## Template (Kahn's)
```java
int[] indeg = new int[n];
List<Integer>[] adj = /* build adjacency */;
for (each edge u->v) indeg[v]++;
Queue<Integer> q = new LinkedList<>();
for (int i = 0; i < n; i++) if (indeg[i] == 0) q.offer(i);  // sources
List<Integer> order = new ArrayList<>();
while (!q.isEmpty()) {
    int u = q.poll(); order.add(u);
    for (int v : adj[u]) if (--indeg[v] == 0) q.offer(v);    // "remove" u, free dependents
}
// order.size() == n  => valid topo order; else there is a cycle
```

## Problems

### P1. Course schedule — can you finish all courses? (cycle detection)
```java
boolean canFinish(int numCourses, int[][] prereq) {
    List<Integer>[] adj = new List[numCourses];
    for (int i = 0; i < numCourses; i++) adj[i] = new ArrayList<>();
    int[] indeg = new int[numCourses];
    for (int[] p : prereq) { adj[p[1]].add(p[0]); indeg[p[0]]++; } // p[1] -> p[0]
    Queue<Integer> q = new LinkedList<>();
    for (int i = 0; i < numCourses; i++) if (indeg[i] == 0) q.offer(i);
    int done = 0;
    while (!q.isEmpty()) {
        int u = q.poll(); done++;
        for (int v : adj[u]) if (--indeg[v] == 0) q.offer(v);
    }
    return done == numCourses;                 // all taken => no cycle
}
```
**Why:** if a cycle exists, those nodes never reach in-degree 0, so `done < numCourses`. **Time O(V + E), Space O(V + E).**

### P2. Course schedule II — return a valid order
```java
int[] findOrder(int numCourses, int[][] prereq) {
    List<Integer>[] adj = new List[numCourses];
    for (int i = 0; i < numCourses; i++) adj[i] = new ArrayList<>();
    int[] indeg = new int[numCourses];
    for (int[] p : prereq) { adj[p[1]].add(p[0]); indeg[p[0]]++; }
    Queue<Integer> q = new LinkedList<>();
    for (int i = 0; i < numCourses; i++) if (indeg[i] == 0) q.offer(i);
    int[] order = new int[numCourses]; int idx = 0;
    while (!q.isEmpty()) {
        int u = q.poll(); order[idx++] = u;
        for (int v : adj[u]) if (--indeg[v] == 0) q.offer(v);
    }
    return idx == numCourses ? order : new int[0];  // empty => cycle, no valid order
}
```
**Time O(V + E), Space O(V + E).**

### P3. Alien dictionary (derive letter order from sorted words)
```java
String alienOrder(String[] words) {
    Map<Character, Set<Character>> adj = new HashMap<>();
    Map<Character, Integer> indeg = new HashMap<>();
    for (String w : words) for (char c : w.toCharArray()) { adj.putIfAbsent(c, new HashSet<>()); indeg.putIfAbsent(c, 0); }
    for (int i = 0; i + 1 < words.length; i++) {
        String a = words[i], b = words[i+1];
        int len = Math.min(a.length(), b.length()), j = 0;
        while (j < len && a.charAt(j) == b.charAt(j)) j++;
        if (j == len) { if (a.length() > b.length()) return ""; }   // invalid: prefix after longer
        else if (adj.get(a.charAt(j)).add(b.charAt(j))) indeg.merge(b.charAt(j), 1, Integer::sum);
    }
    Queue<Character> q = new LinkedList<>();
    for (var e : indeg.entrySet()) if (e.getValue() == 0) q.offer(e.getKey());
    StringBuilder sb = new StringBuilder();
    while (!q.isEmpty()) {
        char c = q.poll(); sb.append(c);
        for (char nx : adj.get(c)) if (indeg.merge(nx, -1, Integer::sum) == 0) q.offer(nx);
    }
    return sb.length() == indeg.size() ? sb.toString() : "";        // cycle => invalid
}
```
**Why:** adjacent words give one ordering constraint (the first differing char); topo-sort the letters. **Time O(total chars), Space O(1)** (bounded alphabet). A classic hard problem — the edge cases (invalid prefix) matter.

## Follow-ups & optimizations
- *Kahn's vs DFS?* → Kahn's naturally detects cycles (count processed nodes) and gives a lexicographically controllable order (use a `PriorityQueue` for the smallest valid order); DFS is concise but you must track the recursion stack for cycle detection.
- *"Smallest/lexicographically-first valid order?"* → replace the queue with a **min-heap** in Kahn's.
- *"Parallel scheduling — minimum time with unlimited workers?"* → the number of BFS levels in Kahn's = the longest dependency chain (critical path).
- Cycle = no valid order: always handle it (return empty / false).

## Complexity summary
**O(V + E) time, O(V + E) space.** Topo-sort is just BFS/DFS with the in-degree (or finish-order) twist; recognizing "dependencies → DAG ordering" is the identification skill.
