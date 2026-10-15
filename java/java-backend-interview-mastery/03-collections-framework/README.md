# 03 — Collections Framework

> **Priority: P0.** "Which collection and why" plus **HashMap internals** are among the most reliable SDE-2 questions because they reveal whether you understand hashing, memory, complexity, and thread-safety — not just API names.

**Prerequisites:** [01 Core Java](../01-core-java/) (equals/hashCode, immutability).
**Related:** [06 Concurrency](../06-java-concurrency/) (ConcurrentHashMap, fail-fast), [04 Generics](../04-generics-and-type-system/).

---

## Table of contents
1. [Big picture & choosing a collection](#1-big-picture)
2. [List: ArrayList vs LinkedList](#2-list)
3. [HashMap internals](#3-hashmap-internals)
4. [LinkedHashMap & TreeMap](#4-linkedhashmap--treemap)
5. [Sets](#5-sets)
6. [Hashtable, ConcurrentHashMap & thread-safety](#6-concurrency)
7. [Queues & Deques](#7-queues--deques)
8. [Comparable vs Comparator](#8-comparable-vs-comparator)
9. [Iterators & fail-fast](#9-iterators--fail-fast)
10. [Immutable & unmodifiable collections](#10-immutable--unmodifiable)
11. [Trade-offs](#d-trade-offs)
12. [Interview Q&A](#e-interview-qa)
13. [Production relevance](#f-production-relevance)
14. [Revision](#g-revision)

---

## 1. Big picture

### A. Concept & intuition
The Collections Framework is a set of interfaces (`List`, `Set`, `Map`, `Queue`, `Deque`) with multiple implementations trading off ordering, access cost, memory, and concurrency. The engineering problem it solved: before it (early Java), you hand-rolled data structures or used `Vector`/`Hashtable` (synchronized, slow, inconsistent APIs). The framework gave uniform, composable, mostly-unsynchronized implementations so you pick the structure that fits the access pattern.

### Decision guide (say the *reason*, not just the name)
| Need | Pick | Why |
|---|---|---|
| Index access, iteration, append | `ArrayList` | contiguous array, O(1) random access, cache-friendly |
| Key→value lookup | `HashMap` | O(1) average by hash |
| Lookup + insertion order | `LinkedHashMap` | hash + doubly-linked list |
| Lookup + sorted keys | `TreeMap` | red-black tree, O(log n), range queries |
| Uniqueness | `HashSet` (`LinkedHashSet`/`TreeSet` for order) | backed by a map |
| FIFO / work queue | `ArrayDeque` / `LinkedBlockingQueue` | deque or blocking handoff |
| Priority ordering | `PriorityQueue` | binary heap |
| Concurrent map | `ConcurrentHashMap` | per-bin locking/CAS |

---

## 2. List

### ArrayList vs LinkedList — complexity & reality
| Operation | ArrayList | LinkedList |
|---|---|---|
| get(i) | O(1) | O(n) (walk nodes) |
| add (end, amortized) | O(1) | O(1) |
| add/remove at index i | O(n) (shift) | O(n) to *find* + O(1) to relink |
| memory per element | compact (array slot) | heavy (node object + 2 refs) |
| cache locality | excellent | poor (pointer chasing) |

**Interview reality:** `LinkedList` is almost always the wrong default. People reach for it "because inserts are O(1)," but you still pay O(n) to *reach* the position, and the pointer-chasing + per-node object overhead usually makes `ArrayList` faster even for mid-list inserts at realistic sizes. Use `LinkedList`/`ArrayDeque` only when you genuinely need a queue/deque — and `ArrayDeque` beats `LinkedList` as a deque. (Don't invent numbers; say "measure, but ArrayList is the safe default.")

**ArrayList growth:** backed by an array; when full it grows (~1.5× in the OpenJDK implementation — an implementation detail, not a spec guarantee) and copies. Preallocate with `new ArrayList<>(expectedSize)` when you know the size to avoid repeated copies.

---

## 3. HashMap internals

### A. Concept & intuition
`HashMap` maps keys to values in amortized O(1) by converting a key to a bucket index via its hash. The whole thing hinges on `hashCode()` and `equals()` being correct and consistent (see [01 §equals/hashCode](../01-core-java/)).

### B. Internal working — `put(key, value)` step by step
1. Compute `h = key.hashCode()`, then **spread** it: `h ^ (h >>> 16)`. This mixes high bits into low bits so that keys whose hashes differ only in high bits don't all collide (the table size is a power of two and the index uses only low bits).
2. Index = `(n - 1) & h` where `n` is the current table length (power of two → bitmask instead of modulo).
3. If the bucket is empty, place a new node.
4. If occupied (collision), walk the bucket:
   - As a **linked list** of nodes, comparing with `equals` (after a fast `hash ==` check). If a key equals an existing one, replace its value.
   - If the bucket's chain length reaches the **treeify threshold (8)** *and* the table size is ≥ 64, the bin converts to a **red-black tree** (O(log n) instead of O(n) for a pathological bucket). It untreeifies back to a list below ~6. **This is an implementation detail of OpenJDK HashMap since Java 8** — state it as such, not as a language guarantee.
5. Increment size; if `size > capacity * loadFactor` (default load factor 0.75), **resize**: allocate a table of double capacity and **rehash**/redistribute entries.

### Capacity, load factor, resizing
- Default initial capacity 16, load factor 0.75 → resizes at 12 entries, then 24, etc.
- Resizing is O(n) and allocates a new array; frequent resizing hurts. If you know you'll hold ~1000 entries, size it: `new HashMap<>(1431)` roughly (initialCapacity so that `cap*0.75 ≥ 1000`; actual table rounds up to a power of two). Over-sizing wastes memory; under-sizing causes rehash churn.
- Keys must be effectively immutable in the fields used by `hashCode`/`equals` — a mutating key moves buckets conceptually but the entry stays put, so you "lose" it (see CJ-09 / COL-07).

### C. Code — collision & a broken key
```java
// BROKEN: equals/hashCode inconsistent -> entries get lost
class BadKey {
    final int id;
    BadKey(int id){ this.id = id; }
    @Override public boolean equals(Object o){ return o instanceof BadKey b && b.id==id; }
    // no hashCode override -> uses Object identity hash
}
var map = new java.util.HashMap<BadKey,String>();
map.put(new BadKey(1), "a");
map.get(new BadKey(1)); // null! different identity hash -> different bucket -> never found
```
```java
// FIXED: hashCode consistent with equals
class GoodKey {
    final int id;
    GoodKey(int id){ this.id = id; }
    @Override public boolean equals(Object o){ return o instanceof GoodKey g && g.id==id; }
    @Override public int hashCode(){ return Integer.hashCode(id); }
}
```
Observable behavior: with `GoodKey`, `get(new GoodKey(1))` returns `"a"`. The rule: **if two objects are equal they must have the same hashCode**; the reverse need not hold (collisions are allowed and handled).

---

## 4. LinkedHashMap & TreeMap
- **`LinkedHashMap`**: HashMap + a doubly-linked list threading entries in **insertion order** (or **access order** if constructed with `accessOrder=true` — the basis of a simple **LRU cache** by overriding `removeEldestEntry`). Slightly more memory than HashMap.
- **`TreeMap`**: red-black tree, keys kept **sorted** by natural order or a `Comparator`; O(log n) ops; supports range views (`headMap`, `tailMap`, `subMap`, `firstKey`, `ceilingKey`). Use when you need ordering or range queries, not raw lookup speed. `null` keys not allowed (needs comparison).

```java
// Minimal LRU cache via LinkedHashMap access-order
class LruCache<K,V> extends java.util.LinkedHashMap<K,V> {
    private final int cap;
    LruCache(int cap){ super(16, 0.75f, true); this.cap = cap; } // accessOrder=true
    @Override protected boolean removeEldestEntry(java.util.Map.Entry<K,V> e){ return size() > cap; }
}
```

---

## 5. Sets
`HashSet`, `LinkedHashSet`, `TreeSet` are thin wrappers over the corresponding maps (value is a dummy). Same ordering/complexity trade-offs. Element uniqueness relies on `equals`/`hashCode` (hash sets) or `compareTo`/`Comparator` (tree set) — **and a `TreeSet` considers two elements equal if `compareTo` returns 0, even if `equals` says otherwise**, a classic trap.

---

## 6. Concurrency

### Why `HashMap` is not thread-safe (COL-01 core)
Concurrent mutation has two failure modes:
1. **Lost updates / visibility:** no happens-before edge, so writes can be lost or unseen.
2. **Structural corruption during resize:** two threads resizing simultaneously can corrupt the bucket chains. In Java 7 this could create a cycle in a bucket's linked list, causing a subsequent `get` to **loop forever** (100% CPU). Java 8 changed resize to preserve order and reduce (not eliminate) this specific failure, but `HashMap` remains unsafe for concurrent writes.

### `Hashtable` vs `Collections.synchronizedMap` vs `ConcurrentHashMap`
- `Hashtable`: legacy; every method `synchronized` on the whole table → coarse lock, poor throughput. Avoid.
- `Collections.synchronizedMap(map)`: wraps each method in a single lock; iteration still needs external synchronization. Coarse.
- **`ConcurrentHashMap`**: fine-grained — operations lock/CAS at the level of individual bins, so unrelated keys don't contend. Reads are largely lock-free. Does **not** allow `null` keys/values (so a `null` from `get` unambiguously means "absent").

### The critical subtlety — ConcurrentHashMap is NOT a universal fix
Each *single* operation is atomic, but a **compound** operation is still a race:
```java
// STILL BROKEN even with ConcurrentHashMap: get-then-put is two operations
if (!chm.containsKey(k)) chm.put(k, compute()); // two threads both pass the check
// FIXED: use the atomic method
chm.computeIfAbsent(k, key -> compute());        // atomic, computed at most once per key
chm.merge(k, 1L, Long::sum);                      // atomic counter increment
chm.putIfAbsent(k, v);
```
So "I used ConcurrentHashMap" does not make your logic correct — it changes *which* operations are atomic. Use its atomic combinators.

---

## 7. Queues & Deques
- `ArrayDeque`: resizable-array deque; the preferred stack and FIFO queue (faster than `Stack`/`LinkedList`); no `null`.
- `PriorityQueue`: binary min-heap by natural order/`Comparator`; `peek`/`poll` O(1)/O(log n); **not** sorted on iteration.
- `BlockingQueue` family (`ArrayBlockingQueue` bounded, `LinkedBlockingQueue` optionally bounded, `SynchronousQueue` handoff): producer/consumer with blocking + backpressure (see [06 §8](../06-java-concurrency/)).

---

## 8. Comparable vs Comparator
- **`Comparable<T>`** (`compareTo`): the type's **natural ordering**, one per class. Implement when there's an obvious default order.
- **`Comparator<T>`**: external, multiple orderings, composable: `Comparator.comparing(User::lastName).thenComparing(User::firstName).reversed()`.
- **Contract:** must be consistent, total, and ideally **consistent with equals** (if not, `TreeMap`/`TreeSet` behave surprisingly). Avoid `a - b` subtraction comparators (integer overflow bug); use `Integer.compare(a,b)`.

---

## 9. Iterators & fail-fast
`ArrayList`/`HashMap` iterators are **fail-fast**: they track a `modCount`; if the collection is structurally modified during iteration (except via the iterator's own `remove`), the next `next()` throws `ConcurrentModificationException`. This is a **best-effort bug detector**, *not* a guarantee — it can miss, and it can fire even in single-threaded code if you modify while iterating.

```java
// BROKEN: CME in single thread
for (String s : list) { if (s.isBlank()) list.remove(s); } // ConcurrentModificationException
// FIXED options:
list.removeIf(String::isBlank);                 // cleanest
// or iterator.remove(); or collect-then-removeAll
```
Concurrent collections use **weakly consistent** iterators instead: they don't throw CME, reflect some but not necessarily all concurrent modifications, and never corrupt.

---

## 10. Immutable & unmodifiable
- `List.of()`, `Map.of()`, `Set.of()` (Java 9+): truly **immutable**, reject `null`, throw on mutation.
- `Collections.unmodifiableList(x)`: an unmodifiable **view** — if someone holds the backing list, they can still mutate it (and your view reflects it). Not the same as immutable.
- `List.copyOf(x)`: defensive immutable copy. Prefer immutable collections for shared/returned data (safe publication, see [06 §3](../06-java-concurrency/)).

---

## D. Trade-offs

| Structure | Lookup | Ordered? | Thread-safe? | Memory | Use when | Poor choice when |
|---|---|---|---|---|---|---|
| ArrayList | O(1) index | insertion | no | low | default list, index access | frequent mid-list structural edits at scale |
| LinkedList | O(n) index | insertion | no | high | rarely; as a deque | random access / general list |
| HashMap | O(1) avg | none | no | medium | general key→value | need ordering / concurrency |
| LinkedHashMap | O(1) avg | insertion/access | no | medium+ | predictable iteration, LRU | memory-critical |
| TreeMap | O(log n) | sorted | no | medium | ranges / sorted keys | pure fast lookup |
| HashSet | O(1) avg | none | no | medium | uniqueness | ordering needed |
| ConcurrentHashMap | O(1) avg | none | yes (per-op) | medium+ | concurrent map | you need compound-op atomicity for free (you don't get it) |
| ArrayDeque | — | insertion | no | low | stack/queue | need indexing |
| PriorityQueue | — | heap | no | low | top-k / scheduling | need sorted iteration |

---

## E. Interview Q&A

<a id="col-01"></a>
### COL-01 (P0) — Why is `HashMap` not thread-safe?
**Testing:** do you know the concrete failure modes, not just the slogan?
**Strong answer:** Two reasons. First, without synchronization there's no happens-before edge, so concurrent `put`s can lose updates or be invisible to other threads. Second, `put` can trigger a **resize**, and two threads resizing concurrently can corrupt the bucket chains; in Java 7 this could form a cycle in a bucket, making a later `get` spin forever at 100% CPU. Java 8 reworked resize to preserve ordering and reduce that specific hazard, but `HashMap` is still unsafe for concurrent writes. Use `ConcurrentHashMap`.
**Weak answer:** "Because it's not synchronized." True but shallow; the interviewer wants the failure mechanism (lost updates + resize corruption).
**Follow-up:** Is read-only concurrent access safe? (If the map is fully built and safely published and never mutated, concurrent reads are fine.)

<a id="col-02"></a>
### COL-02 (P0) — How does `HashMap` handle collisions?
**Strong answer:** Keys landing in the same bucket are chained. Originally a linked list compared via `hash==` then `equals`. Since Java 8, if a single bucket's chain reaches length 8 **and** the table has ≥ 64 buckets, that bucket converts to a red-black tree, bounding worst-case lookup at O(log n) instead of O(n) — protection against hash-collision DoS and pathological hashers. It reverts to a list when the bucket shrinks (~6). That treeification is an OpenJDK implementation detail of Java 8+, not a language guarantee.
**Follow-up:** Why both thresholds (8 and 64)? (Below 64 buckets, resize is cheaper and preferred over treeifying; treeify is for genuinely collision-heavy buckets.)

<a id="col-03"></a>
### COL-03 (P0) — Why does `ConcurrentHashMap` behave differently, and why isn't it a universal replacement?
**Strong answer:** It provides thread-safety with fine-grained concurrency — individual bins are guarded by CAS/synchronized on the bin head, so operations on different keys rarely contend and reads are mostly lock-free; it forbids `null` keys/values. But each *single* operation is atomic — a **compound** operation like `containsKey` then `put` is still a race. You must use its atomic combinators (`computeIfAbsent`, `merge`, `putIfAbsent`). So swapping `HashMap` for `ConcurrentHashMap` fixes data-structure corruption but **not** application-level check-then-act races.
**Trap:** claiming it makes any sequence of map calls atomic.

### COL-04 (P1) — `ArrayList` vs `LinkedList`: which and why?
**Strong answer:** Default to `ArrayList`: O(1) random access, compact, cache-friendly; amortized O(1) append. `LinkedList` has O(1) relink but O(n) to *reach* the index and heavy per-node overhead with poor locality, so in practice it's usually slower except as a genuine deque — and even then `ArrayDeque` is better. I'd only use `LinkedList` if I specifically needed a `Deque`/`Queue` with frequent head/tail ops and chose it over `ArrayDeque` for a concrete reason.

### COL-05 (P1) — `Comparable` vs `Comparator`?
**Strong answer:** `Comparable` defines a type's single natural ordering (`compareTo`); `Comparator` is external and lets you define multiple, composable orderings without touching the class. For sorting by several keys I use `Comparator.comparing(...).thenComparing(...)`. I avoid `a-b` comparators because of integer overflow; I use `Integer.compare`. If a `Comparator` is inconsistent with `equals`, `TreeSet`/`TreeMap` can "drop" elements it considers equal.

<a id="col-06"></a>
### COL-06 (P0) — What is fail-fast iteration and when does `ConcurrentModificationException` happen?
**Strong answer:** Standard collections track a `modCount`; their iterators check it and throw CME if the collection is structurally modified during iteration other than through the iterator's own `remove`. It's a best-effort **bug detector**, not a thread-safety mechanism — it can fire in single-threaded code (removing from a list inside a for-each) and can also miss races. Fixes: `removeIf`, the iterator's `remove`, or iterate a copy. Concurrent collections use weakly-consistent iterators that don't throw and never corrupt.

### COL-07 (P1) — What happens if you mutate a key after putting it in a `HashMap`?
**Strong answer:** If you change fields that affect `hashCode`/`equals`, the entry stays in its original bucket, but lookups now compute a different bucket index → you can't find it, and it leaks (not removed by `remove(key)` either). This is why map keys should be effectively immutable. (Cross-ref CJ-09.)

### COL-08 (P2) — How would you build an LRU cache from the JDK?
**Strong answer:** `LinkedHashMap` with `accessOrder=true` and override `removeEldestEntry` to evict past capacity (single-threaded). For concurrency, use a purpose-built cache (Caffeine) rather than hand-rolling, because correct concurrent LRU eviction is subtle (see [20 Caching](../20-caching-and-performance/)).

---

## F. Production relevance
Collection choice is a quiet performance lever: an `ArrayList` where you needed O(1) membership (should've been a `HashSet`) turns an O(n) loop into O(n²); an unbounded `HashMap` cache with no eviction is a slow memory leak (ch.07); a `HashMap` shared across request threads is a latent corruption/CPU-spin bug (ch.06); a broken `equals`/`hashCode` on an entity silently drops set/map entries and breaks JPA (ch.12). The ConcurrentHashMap compound-op trap is a common source of duplicate processing. Knowing the internals lets you capacity-plan and avoid rehash churn on hot paths.

---

## G. Revision

### 10 key takeaways
1. Pick the collection by access pattern; `ArrayList` and `HashMap` are the right defaults.
2. HashMap: spread hash → `(n-1)&h` bucket → chain → treeify at 8 (and ≥64 buckets) since Java 8.
3. Resize at `size > cap*0.75`, doubling capacity; preallocate to avoid churn.
4. equals/hashCode must be consistent or entries get lost.
5. HashMap isn't thread-safe: lost updates + resize corruption (pre-8 infinite loop).
6. ConcurrentHashMap makes single ops atomic, not compound ones — use `computeIfAbsent`/`merge`.
7. `LinkedList` is rarely right; `ArrayDeque` beats it as a deque.
8. TreeMap/TreeSet order by comparator; equal-by-compareTo means "same element."
9. Fail-fast iterators are bug detectors, not thread-safety.
10. Prefer `List.of`/`Map.of`/`copyOf` for shared immutable data.

### 10 rapid-fire
1. Default load factor? (0.75) 2. Treeify threshold? (8, with ≥64 buckets) 3. Does ConcurrentHashMap allow null? (no) 4. O(1) random access list? (ArrayList) 5. Sorted map? (TreeMap) 6. Insertion-order map? (LinkedHashMap) 7. Atomic increment in a map? (`merge`) 8. Fix a for-each removal? (`removeIf`) 9. Preferred deque? (ArrayDeque) 10. Natural ordering interface? (Comparable)

### 5 tricky
1. Why can bad hashCode make HashMap O(n)? (all keys one bucket; treeify caps at O(log n) only if ≥64 buckets)
2. Does swapping to ConcurrentHashMap fix `containsKey`+`put`? (no)
3. Why might a TreeSet silently ignore an add? (compareTo returned 0)
4. Can CME happen single-threaded? (yes)
5. Why does a mutated key leak? (bucket index diverges)

### 5 exercises
1. Reproduce entry loss with a key missing `hashCode`; fix and verify.
2. Capacity-plan a HashMap for 10k entries; show resize count difference via logging.
3. Convert an O(n²) `List.contains` loop to a `HashSet`.
4. Trigger CME with for-each removal; fix three ways.
5. Build the `LinkedHashMap` LRU cache and test eviction.

### One-page revision sheet
> **ArrayList/HashMap = defaults.** HashMap put: spread `h^(h>>>16)` → `(n-1)&h` → chain → treeify(8 & ≥64 buckets, Java 8+). Resize at 0.75 load, doubles. equals⇒same hashCode. Not thread-safe (lost updates + resize corruption). ConcurrentHashMap: per-bin, no null, single-op atomic only → use `computeIfAbsent`/`merge`/`putIfAbsent`. TreeMap=sorted O(log n); compareTo==0 means equal. Fail-fast=modCount bug detector (can fire single-threaded; use `removeIf`). Immutable: `List.of`/`copyOf`. LinkedList rarely; ArrayDeque for deque.

### Prerequisites to revisit
- equals/hashCode & immutability (ch.01).
- Happens-before & ConcurrentHashMap atomic ops (ch.06).
