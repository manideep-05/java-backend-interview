# Pattern 12 — Tree BFS / DFS

## Identify it when
- The input is a **binary tree** (or n-ary) and you need **level-order** info, **path** computations, or **traversals**.
- **BFS (queue)** when the answer is **per level**: "level order", "right side view", "zigzag", "minimum depth", "connect level nodes".
- **DFS (recursion/stack)** when the answer is about **paths or subtree aggregates**: "max depth", "path sum", "diameter", "LCA", "serialize".

```java
class TreeNode { int val; TreeNode left, right; TreeNode(int v){ val = v; } }
```

## Templates
```java
// BFS level order
Queue<TreeNode> q = new LinkedList<>();
if (root != null) q.offer(root);
while (!q.isEmpty()) {
    int sz = q.size();                       // nodes in this level
    for (int i = 0; i < sz; i++) {
        TreeNode n = q.poll();
        if (n.left != null) q.offer(n.left);
        if (n.right != null) q.offer(n.right);
    }
}
// DFS recursion
void dfs(TreeNode n) { if (n == null) return; /* preorder */ dfs(n.left); /* inorder */ dfs(n.right); /* postorder */ }
```

## Problems

### P1. Level order traversal (BFS)
```java
List<List<Integer>> levelOrder(TreeNode root) {
    List<List<Integer>> res = new ArrayList<>();
    Queue<TreeNode> q = new LinkedList<>();
    if (root != null) q.offer(root);
    while (!q.isEmpty()) {
        int sz = q.size();
        List<Integer> level = new ArrayList<>();
        for (int i = 0; i < sz; i++) {
            TreeNode n = q.poll();
            level.add(n.val);
            if (n.left != null) q.offer(n.left);
            if (n.right != null) q.offer(n.right);
        }
        res.add(level);
    }
    return res;
}
```
**Why:** snapshot `q.size()` at the start of each level to separate levels. **Time O(n), Space O(n)** (queue up to the widest level). Right-side view = last node of each level; zigzag = reverse alternate levels.

### P2. Maximum depth (DFS)
```java
int maxDepth(TreeNode root) {
    if (root == null) return 0;
    return 1 + Math.max(maxDepth(root.left), maxDepth(root.right));
}
```
**Time O(n), Space O(h)** (recursion stack; h = height, O(n) worst for a skewed tree).

### P3. Path sum II — all root-to-leaf paths summing to target (DFS + backtracking)
```java
List<List<Integer>> pathSum(TreeNode root, int target) {
    List<List<Integer>> res = new ArrayList<>();
    dfs(root, target, new ArrayList<>(), res);
    return res;
}
void dfs(TreeNode n, int rem, List<Integer> path, List<List<Integer>> res) {
    if (n == null) return;
    path.add(n.val); rem -= n.val;
    if (n.left == null && n.right == null && rem == 0) res.add(new ArrayList<>(path)); // leaf hit
    else { dfs(n.left, rem, path, res); dfs(n.right, rem, path, res); }
    path.remove(path.size() - 1);                  // backtrack
}
```
**Time O(n) nodes × O(h) copy, Space O(h).** The `path.remove` undo is the backtracking link to pattern 17.

### P4. Diameter of a binary tree (DFS returning height, updating a global)
```java
int diameter = 0;
int diameterOfBinaryTree(TreeNode root) { height(root); return diameter; }
int height(TreeNode n) {
    if (n == null) return 0;
    int l = height(n.left), r = height(n.right);
    diameter = Math.max(diameter, l + r);          // longest path through this node
    return 1 + Math.max(l, r);
}
```
**Why:** a single post-order pass computes each node's height while tracking the longest `left+right` path. **Time O(n), Space O(h).** This "compute X while returning Y" shape recurs (balanced tree, max path sum).

### P5. Lowest common ancestor (binary tree, DFS)
```java
TreeNode lowestCommonAncestor(TreeNode root, TreeNode p, TreeNode q) {
    if (root == null || root == p || root == q) return root;
    TreeNode l = lowestCommonAncestor(root.left, p, q);
    TreeNode r = lowestCommonAncestor(root.right, p, q);
    if (l != null && r != null) return root;       // p and q split here -> this is the LCA
    return l != null ? l : r;                       // both on one side
}
```
**Time O(n), Space O(h).**

## Follow-ups & optimizations
- *"Traverse without recursion?"* → iterative DFS with an explicit `Deque` stack (needed if the tree is very deep and the recursion stack could overflow).
- *"Morris traversal?"* → inorder in **O(1) space** by threading temporary links — a strong advanced answer.
- *BFS vs DFS choice:* per-level or shortest-depth → BFS; path/subtree aggregate → DFS. Say which and why.
- *Serialize/deserialize:* preorder DFS with null markers, or BFS — common hard follow-up.
- Space is dominated by recursion depth O(h); mention it degrades to O(n) for a skewed tree.

## Complexity summary
Almost all tree traversals: **O(n) time** (visit each node once), **O(h) space** for DFS recursion (O(n) worst-case skewed) or **O(width)** for the BFS queue.
