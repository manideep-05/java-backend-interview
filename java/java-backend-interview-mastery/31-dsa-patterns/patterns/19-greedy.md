# Pattern 19 — Greedy

## Identify it when
- At each step there's an obvious **locally optimal choice**, and taking it repeatedly yields a **global optimum**.
- "Minimum number of …", "maximum you can …", "schedule the most …", "can you reach …", "least intervals/arrows/platforms".
- The problem has **optimal substructure** AND the **greedy-choice property** (a local optimum is part of some global optimum). If greedy *looks* right but you can't argue it, suspect DP instead.

**Core idea:** sort by the right key (often end time, ratio, or deadline), then make the greedy choice in one pass. **The hard part is proving it** — be ready with an **exchange argument** ("swapping to the greedy choice never makes the solution worse").

## Problems

### P1. Jump game — can you reach the last index? (greedy reach)
```java
boolean canJump(int[] a) {
    int farthest = 0;
    for (int i = 0; i < a.length; i++) {
        if (i > farthest) return false;              // unreachable gap
        farthest = Math.max(farthest, i + a[i]);     // extend the reachable frontier
    }
    return true;
}
```
**Why:** track the farthest index reachable so far; if the current index exceeds it, you're stuck. **Time O(n), Space O(1).** (DP would be O(n²) — greedy is the optimization.)

### P2. Jump game II — minimum jumps (greedy BFS-by-levels)
```java
int jump(int[] a) {
    int jumps = 0, curEnd = 0, farthest = 0;
    for (int i = 0; i < a.length - 1; i++) {
        farthest = Math.max(farthest, i + a[i]);
        if (i == curEnd) { jumps++; curEnd = farthest; }  // must jump; extend the level boundary
    }
    return jumps;
}
```
**Why:** treat each "jump range" as a BFS level; increment jumps when you exhaust the current range. **Time O(n), Space O(1).**

### P3. Gas station — can you complete the circuit?
```java
int canCompleteCircuit(int[] gas, int[] cost) {
    int total = 0, tank = 0, start = 0;
    for (int i = 0; i < gas.length; i++) {
        int diff = gas[i] - cost[i];
        total += diff; tank += diff;
        if (tank < 0) { start = i + 1; tank = 0; }   // can't reach i+1 from current start -> restart
    }
    return total < 0 ? -1 : start;
}
```
**Why:** if total gas ≥ total cost a solution exists; the first station after any failing prefix is the unique valid start. **Time O(n), Space O(1).** The greedy insight (restart after a deficit) is the whole trick.

### P4. Non-overlapping intervals — min removals (activity selection)
```java
int eraseOverlapIntervals(int[][] iv) {
    Arrays.sort(iv, (a, b) -> Integer.compare(a[1], b[1]));  // sort by END
    int kept = 0; long end = Long.MIN_VALUE;
    for (int[] x : iv) {
        if (x[0] >= end) { kept++; end = x[1]; }    // greedily keep the earliest-finishing
    }
    return iv.length - kept;                         // removals = total - kept
}
```
**Why (exchange argument):** choosing the interval that **finishes earliest** leaves the most room for the rest — any other first choice can be swapped for it without reducing the count. **Time O(n log n), Space O(1).** Same idea: meeting rooms, arrows (pattern 07).

### P5. Task scheduler — minimum intervals with cooldown
```java
int leastInterval(char[] tasks, int n) {
    int[] freq = new int[26];
    for (char c : tasks) freq[c - 'A']++;
    int maxFreq = 0, countMax = 0;
    for (int f : freq) { if (f > maxFreq) { maxFreq = f; countMax = 1; } else if (f == maxFreq) countMax++; }
    int slots = (maxFreq - 1) * (n + 1) + countMax;  // fill around the most frequent task
    return Math.max(tasks.length, slots);
}
```
**Why:** the most frequent task dictates the skeleton of idle slots; fill the rest greedily. **Time O(tasks), Space O(1).**

## Follow-ups & optimizations
- *"Prove it's optimal."* → give an **exchange argument**: assume an optimal solution differs from the greedy choice, swap in the greedy choice, show the result is no worse. This is the #1 thing interviewers press on; a greedy answer without justification is risky.
- *"When does greedy fail?"* → when a locally optimal choice forecloses a better global one — e.g., **coin change with arbitrary denominations** (greedy largest-coin fails for coins like {1,3,4}, amount 6) → that's DP (pattern 18). Knowing *when greedy breaks* is as valued as knowing when it works.
- *Sort key is the crux:* by end time (scheduling), by ratio (fractional knapsack), by deadline (job sequencing). Choosing the wrong key gives a wrong greedy.

## Complexity summary
Usually **O(n log n)** (a sort) **+ O(n)** sweep, **O(1)** extra space — the appeal over DP. But greedy requires a correctness argument; if you can't justify the greedy-choice property, fall back to DP. Greedy is fast but only correct for problems that have the property.
