# Pattern 13 — Binary Search Tree (BST)

## Identify it when
- The tree is a **BST** (left subtree < node < right subtree) and you exploit the ordering.
- "Validate BST", "kth smallest/largest", "insert/delete/search in BST", "LCA in a BST", "range sum", "convert sorted array to BST".
- Key fact to use: **an inorder traversal of a BST yields values in sorted order.**

## Templates
```java
// Ordered navigation: at each node, go left if target < node, right if target > node
TreeNode node = root;
while (node != null) {
    if (target == node.val) break;
    node = target < node.val ? node.left : node.right;   // O(h) search
}

// Inorder = sorted sequence
void inorder(TreeNode n) { if (n == null) return; inorder(n.left); visit(n.val); inorder(n.right); }
```

## Problems

### P1. Validate BST (bounds, not just parent comparison)
```java
boolean isValidBST(TreeNode root) { return valid(root, Long.MIN_VALUE, Long.MAX_VALUE); }
boolean valid(TreeNode n, long lo, long hi) {
    if (n == null) return true;
    if (n.val <= lo || n.val >= hi) return false;          // must fit the allowed open range
    return valid(n.left, lo, n.val) && valid(n.right, n.val, hi); // tighten bounds going down
}
```
**Why (the trap):** comparing only to the immediate parent is wrong — every node must satisfy an inherited `(lo, hi)` range from all ancestors. Use `long` bounds to handle `Integer.MIN/MAX_VALUE` node values. **Time O(n), Space O(h).**

### P2. Kth smallest element (inorder, early stop)
```java
int kthSmallest(TreeNode root, int k) {
    Deque<TreeNode> st = new ArrayDeque<>();
    TreeNode cur = root;
    while (cur != null || !st.isEmpty()) {
        while (cur != null) { st.push(cur); cur = cur.left; }  // go left
        cur = st.pop();
        if (--k == 0) return cur.val;                          // kth node in sorted order
        cur = cur.right;
    }
    return -1;
}
```
**Why:** iterative inorder visits nodes in ascending order; stop at the kth. **Time O(h + k), Space O(h).** For kth *largest*, do reverse inorder (right → node → left).

### P3. Insert into a BST
```java
TreeNode insertIntoBST(TreeNode root, int val) {
    if (root == null) return new TreeNode(val);
    if (val < root.val) root.left = insertIntoBST(root.left, val);
    else root.right = insertIntoBST(root.right, val);
    return root;
}
```
**Time O(h), Space O(h).**

### P4. LCA in a BST (use ordering — simpler than the general tree version)
```java
TreeNode lcaBST(TreeNode root, TreeNode p, TreeNode q) {
    TreeNode node = root;
    while (node != null) {
        if (p.val < node.val && q.val < node.val) node = node.left;     // both smaller
        else if (p.val > node.val && q.val > node.val) node = node.right;// both larger
        else return node;                                                // split point = LCA
    }
    return null;
}
```
**Why:** the first node where `p` and `q` diverge (one goes left, one right, or one equals) is the LCA. **Time O(h), Space O(1)** — better than the O(h) stack of the general-tree LCA.

### P5. Convert sorted array to a height-balanced BST
```java
TreeNode sortedArrayToBST(int[] a) { return build(a, 0, a.length - 1); }
TreeNode build(int[] a, int lo, int hi) {
    if (lo > hi) return null;
    int mid = lo + (hi - lo) / 2;                 // middle = root keeps it balanced
    TreeNode n = new TreeNode(a[mid]);
    n.left = build(a, lo, mid - 1);
    n.right = build(a, mid + 1, hi);
    return n;
}
```
**Why:** picking the middle as the root recursively balances the tree. **Time O(n), Space O(log n)** recursion.

## Follow-ups & optimizations
- *"Why is BST search O(h) and when is that bad?"* → O(log n) if balanced, O(n) if skewed (degenerate to a linked list). Self-balancing trees (AVL, Red-Black — Java's `TreeMap`) keep h = O(log n).
- *"Delete a node?"* → three cases: leaf (remove), one child (splice), two children (replace with inorder successor = smallest in right subtree).
- *"Kth largest / range queries?"* → reverse inorder; or augment nodes with subtree sizes for O(log n) order statistics.
- *Validate BST with inorder:* alternative — inorder must be strictly increasing (track the previous value).

## Complexity summary
BST operations: **O(h) time** (O(log n) balanced, O(n) skewed), **O(h) space** for recursion. Inorder-based (kth, validate, sorted output): **O(n) time**. The ordering is what turns O(n) tree problems into O(h).
