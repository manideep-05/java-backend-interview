# Pattern 04 — Fast & Slow Pointers (Floyd's)

## Identify it when
- A **linked list** problem mentions a **cycle**, the **middle**, or the **nth node**.
- A problem reduces to following "next" repeatedly and asks if it **loops** (happy number, cycle in a functional graph).
- You need **O(1) space** cycle detection (a HashSet works but uses O(n)).

**Core idea:** move `slow` one step and `fast` two steps. If there's a cycle they meet; if `fast` hits null there's no cycle. The gap between them closes by 1 each step.

## Template
```java
ListNode slow = head, fast = head;
while (fast != null && fast.next != null) {
    slow = slow.next;
    fast = fast.next.next;
    if (slow == fast) { /* cycle */ break; }
}
```

## Problems

### P1. Detect a cycle in a linked list
```java
boolean hasCycle(ListNode head) {
    ListNode slow = head, fast = head;
    while (fast != null && fast.next != null) {
        slow = slow.next; fast = fast.next.next;
        if (slow == fast) return true;      // pointers meet inside the loop
    }
    return false;
}
```
**Time O(n), Space O(1)** (vs O(n) space with a HashSet of visited nodes).

### P2. Find the cycle start node
```java
ListNode detectCycle(ListNode head) {
    ListNode slow = head, fast = head;
    while (fast != null && fast.next != null) {
        slow = slow.next; fast = fast.next.next;
        if (slow == fast) {                 // meeting point
            ListNode p = head;
            while (p != slow) { p = p.next; slow = slow.next; } // both move 1 step
            return p;                       // meet at the cycle entrance
        }
    }
    return null;
}
```
**Why (the math worth knowing):** if the distance from head to the cycle start is `a` and from the start to the meeting point is `b`, then starting one pointer at head and one at the meeting point, moving both one step, they meet exactly at the cycle start. **Time O(n), Space O(1).**

### P3. Middle of the linked list
```java
ListNode middleNode(ListNode head) {
    ListNode slow = head, fast = head;
    while (fast != null && fast.next != null) { slow = slow.next; fast = fast.next.next; }
    return slow;                            // when fast reaches the end, slow is at the middle
}
```
**Time O(n), Space O(1).**

### P4. Happy number (cycle in a number sequence)
```java
boolean isHappy(int n) {
    int slow = n, fast = next(n);
    while (fast != 1 && slow != fast) { slow = next(slow); fast = next(next(fast)); }
    return fast == 1;
}
int next(int x) { int s = 0; while (x > 0) { int d = x % 10; s += d*d; x /= 10; } return s; }
```
**Why:** the sequence of digit-square-sums either reaches 1 or cycles; Floyd detects the cycle without a set. **Time ~O(log n) per step, Space O(1).**

### P5. Remove the nth node from the end (two pointers, gap of n)
```java
ListNode removeNthFromEnd(ListNode head, int n) {
    ListNode dummy = new ListNode(0, head), fast = dummy, slow = dummy;
    for (int i = 0; i < n; i++) fast = fast.next;   // fast leads by n
    while (fast.next != null) { fast = fast.next; slow = slow.next; }
    slow.next = slow.next.next;                     // slow is just before the target
    return dummy.next;
}
```
**Why:** keep a gap of `n`; when `fast` hits the end, `slow` is right before the node to drop. One pass. **Time O(n), Space O(1).** The **dummy node** avoids special-casing head removal.

## Follow-ups & optimizations
- *"Length of the cycle?"* → after meeting, keep one pointer fixed and advance the other until they meet again, counting steps.
- *"Why fast = slow·2 and not ·3?"* → with speed 2 the gap shrinks by exactly 1 per step, guaranteeing a meeting; other ratios can skip past.
- *"Palindrome linked list in O(1) space?"* → find middle (slow/fast), reverse the second half, compare (combines pattern 04 + 11).
- HashSet alternative is simpler to write but O(n) space — state the trade-off.

## Complexity summary
All variants: **O(n) time, O(1) space** — the space win over a visited-set is the whole point.
