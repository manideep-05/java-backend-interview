# LRU Cache — LLD

A favorite because it blends data-structure design with API design and (at senior level) concurrency.

---

## 1. Requirements

**Functional**
- `get(key)` → value or "miss".
- `put(key, value)` → insert/update.
- Fixed **capacity**; when full, evict the **least recently used** entry.
- Both `get` and `put` count as "using" a key (they refresh recency).

**Non-functional**
- `get` and `put` must be **O(1)**. This constraint drives the whole design.
- Concurrency (senior follow-up): many threads hitting the cache.

---

## 2. Why the data structure is the design

O(1) for both "look up by key" and "move to most-recently-used" forces:
- A **HashMap** `key → node` for O(1) lookup.
- A **doubly linked list** of nodes ordered by recency (head = most recent, tail = least recent). Doubly linked so we can unlink any node in O(1) given its reference.

`get`: map lookup → move node to head.
`put`: if present, update + move to head; else insert at head; if over capacity, drop the tail (and its map entry).

This HashMap-plus-DLL combo *is* the pattern here (it's exactly how `LinkedHashMap` with access-order works).

---

## 3. Critical flows

1. **Hit**: `get` finds the node, splices it out, re-inserts at head, returns value.
2. **Miss**: `get` returns a sentinel/null.
3. **Evict**: `put` on a full cache removes the tail node and its map entry *before/after* inserting the new head.

---

## 4. Edge cases & how to spot them

| Source | Edge | Handling |
|--------|------|----------|
| Boundary | Capacity 0 or 1 | Guard: capacity ≥ 1; capacity 1 evicts on every new key. |
| Duplicate | `put` existing key | Update value + move to head, don't grow size. |
| Empty | `get` on empty cache | Return miss. |
| Null | null key/value | Decide policy (reject, or allow one null — be explicit). |
| Concurrency | Two threads `put` racing an eviction | The whole get/put must be atomic (one lock) — see production. |

**How to find edges:** poke capacity boundaries (0/1/full), repeat keys, and ask "what if two threads do this at once?"

---

## 5. Entities, relationships, responsibilities

- **Node**: holds key, value, prev/next pointers. (We store the key too, so on eviction we can remove it from the map.)
- **DoublyLinkedList** (optional helper): O(1) `addFirst`, `remove`, `removeLast`. *Responsibility:* recency ordering only.
- **LRUCache**: owns the map + list, implements `get`/`put`, enforces capacity. *Responsibility:* the cache policy.
- **(Design extension)** `EvictionPolicy` interface → makes LRU vs LFU vs FIFO a **Strategy** if the interviewer asks to generalize.

---

## 6. Class/interface design

```
LRUCache
 ├── Map<K, Node>        (O(1) lookup)
 └── DoublyLinkedList    (recency order; head=MRU, tail=LRU)
         └── Node { key, value, prev, next }
```

### normal vs production
- `normal/` — single-threaded HashMap + hand-rolled DLL. Clear, O(1), no locks.
- `production/` — thread-safe. The simplest correct approach is **one lock guarding the whole get/put** because each operation mutates *both* the map and the list (so finer-grained locking is error-prone). We also show why a plain `ConcurrentHashMap` is NOT enough on its own (it doesn't maintain recency order atomically with the list).

---

## Interview Questions, Follow-ups & Traps

**Q: Why a doubly (not singly) linked list?**
To unlink an arbitrary node in O(1), you need its predecessor. A doubly linked list gives you `node.prev` directly; a singly linked list would be O(n) to find the predecessor.

**Q: Why store the key inside the node?**
On eviction you remove the tail node, and you must also remove its entry from the map. Without the key on the node you can't know which map entry to delete.

**Q: Can you just use `LinkedHashMap`?**
Yes — `new LinkedHashMap(cap, 0.75f, true)` with `removeEldestEntry` gives access-order LRU. Interviewers usually want the hand-rolled version to see you understand the mechanics, then accept `LinkedHashMap` as the "in practice" answer.

**Q (senior): Make it thread-safe. Is `ConcurrentHashMap` enough?** *(key trap)*
No. A single `get` must atomically (a) read the map and (b) reorder the list. `ConcurrentHashMap` only makes the map safe; the list reorder is a separate mutation, so you'd still corrupt the list under concurrency. You need one lock around the composite operation, or a specialized concurrent design.

**Follow-ups**
- *"How to reduce lock contention?"* — Shard the cache into N segments each with its own lock (key.hashCode() % N), like old `ConcurrentHashMap`. Trade-off: eviction is per-segment, so it's approximate global LRU.
- *"LRU vs LFU?"* — LFU evicts least-*frequently* used (needs frequency counts + a min-frequency structure). Generalize via an `EvictionPolicy` strategy.
- *"TTL / expiry?"* — Add a timestamp per node and lazily evict expired entries on access, plus a background sweeper.

**Traps**
- Using a singly linked list (breaks O(1) eviction/removal).
- Forgetting to update the map on eviction (memory leak + stale lookups).
- Thinking `ConcurrentHashMap` alone makes the cache thread-safe.
- Not refreshing recency on `get` (that's the whole point of LRU).
