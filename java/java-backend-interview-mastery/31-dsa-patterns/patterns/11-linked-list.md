# Pattern 11 — Linked List In-place Reversal

## Identify it when
- A **linked list** problem asks to **reverse** the whole list, a **sublist**, or in **groups of k**, ideally in **O(1) extra space**.
- "Reverse linked list", "reverse between positions m and n", "reverse nodes in k-group", "reorder list", "palindrome linked list".

**Core idea:** rewire `next` pointers with three references — `prev`, `cur`, `next` — moving along the list. A **dummy head** node simplifies edits at the front.

```java
class ListNode { int val; ListNode next; ListNode(int v){ val = v; } ListNode(int v, ListNode n){ val=v; next=n; } }
```

## Template
```java
ListNode prev = null, cur = head;
while (cur != null) {
    ListNode next = cur.next;   // save
    cur.next = prev;            // reverse the link
    prev = cur;                 // advance prev
    cur = next;                 // advance cur
}
return prev;                    // new head
```

## Problems

### P1. Reverse a linked list
```java
ListNode reverseList(ListNode head) {
    ListNode prev = null, cur = head;
    while (cur != null) {
        ListNode next = cur.next;
        cur.next = prev;
        prev = cur; cur = next;
    }
    return prev;
}
```
**Time O(n), Space O(1).** (Recursive version is O(n) space on the call stack — mention the trade-off.)

### P2. Reverse a sublist between positions m and n (1-indexed)
```java
ListNode reverseBetween(ListNode head, int m, int n) {
    ListNode dummy = new ListNode(0, head), prev = dummy;
    for (int i = 1; i < m; i++) prev = prev.next;        // node before the sublist
    ListNode cur = prev.next;
    for (int i = 0; i < n - m; i++) {                    // front-insertion reversal
        ListNode next = cur.next;
        cur.next = next.next;
        next.next = prev.next;
        prev.next = next;
    }
    return dummy.next;
}
```
**Why:** repeatedly move the node after `cur` to the front of the sublist. **Time O(n), Space O(1).** The dummy node handles the case `m == 1`.

### P3. Reverse nodes in k-group
```java
ListNode reverseKGroup(ListNode head, int k) {
    ListNode node = head;
    for (int i = 0; i < k; i++) {                        // check there are k nodes
        if (node == null) return head;                   // fewer than k -> leave as is
        node = node.next;
    }
    ListNode prev = reverseKGroup(node, k);              // reverse the rest first
    ListNode cur = head;
    for (int i = 0; i < k; i++) {                        // reverse this group, linking to prev
        ListNode next = cur.next;
        cur.next = prev;
        prev = cur; cur = next;
    }
    return prev;                                         // new head of this group
}
```
**Time O(n), Space O(n/k) recursion** (or O(1) with an iterative version). A top SDE-2 problem — be ready for both recursive and iterative.

### P4. Palindrome linked list in O(1) space (combines pattern 04 + 11)
```java
boolean isPalindrome(ListNode head) {
    ListNode slow = head, fast = head;
    while (fast != null && fast.next != null) { slow = slow.next; fast = fast.next.next; } // middle
    ListNode second = reverseList(slow);                 // reverse second half
    ListNode p1 = head, p2 = second;
    while (p2 != null) { if (p1.val != p2.val) return false; p1 = p1.next; p2 = p2.next; }
    return true;
}
```
**Why:** find the middle (fast/slow), reverse the second half, compare. **Time O(n), Space O(1).**

### P5. Reorder list (L0→Ln→L1→Ln-1→…)
```java
void reorderList(ListNode head) {
    if (head == null) return;
    ListNode slow = head, fast = head;
    while (fast.next != null && fast.next.next != null) { slow = slow.next; fast = fast.next.next; }
    ListNode second = reverseList(slow.next);            // reverse second half
    slow.next = null;                                    // split
    ListNode p1 = head, p2 = second;
    while (p2 != null) {                                 // weave
        ListNode n1 = p1.next, n2 = p2.next;
        p1.next = p2; p2.next = n1;
        p1 = n1; p2 = n2;
    }
}
```
**Time O(n), Space O(1).**

## Follow-ups & optimizations
- *Recursive vs iterative reversal:* recursive is elegant but O(n) stack; iterative is O(1) — know both.
- *Always use a dummy head* when the head might change (reversal of a prefix, deletion of the head) — it removes special cases.
- *"Reverse k-group iteratively?"* → track the tail of the previous group and the head of the next; more pointer bookkeeping but O(1) space.
- Common bugs: losing the `next` pointer before rewiring; not null-terminating after a split.

## Complexity summary
All variants: **O(n) time.** Iterative reversal **O(1) space**; recursive **O(n) stack**. The in-place pointer rewiring (not building a new list) is the whole point.
