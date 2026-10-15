# In-memory Key-Value Store with TTL — LLD

Design a mini Redis: `put/get/delete` plus **time-to-live expiry**. Tests map design, expiry strategy, and concurrent access — close to real caching work.

---

## 1. Requirements

**Functional**
- `put(key, value)`, `put(key, value, ttl)`, `get(key)`, `delete(key)`.
- Entries with a TTL **expire** and must not be returned after expiry.
- (Optional) max size with eviction (ties into the LRU problem).

**Non-functional**
- O(1) get/put.
- Concurrent readers/writers.
- Memory: expired entries shouldn't linger forever.

---

## 2. Expiry strategies (the core design decision)

| Strategy | How | Pro / Con |
|----------|-----|-----------|
| **Lazy (passive)** | Check expiry on `get`; if expired, remove and return miss. | Cheap, no background work; but expired-but-unread keys waste memory. |
| **Active (sweeper)** | A background thread periodically scans and removes expired keys. | Reclaims memory; costs a thread + scan. |
| **Both (Redis does this)** | Lazy on access + periodic sampling sweep. | Best balance. |

Say you'd combine lazy + a sampling sweeper (exactly Redis's approach).

---

## 3. Critical flow

`get(key)`: look up → if present and `now > expiry`, delete and return miss → else return value.
`put(key, value, ttl)`: store `(value, expiryTime = now + ttl)`.

---

## 4. Edge cases & how to spot them

| Source | Edge | Handling |
|--------|------|----------|
| Expiry | Read an expired key | Lazy delete + miss. |
| TTL | ttl = 0 or no ttl | No expiry (store forever). |
| Clock | Wall-clock jumps | Use `System.nanoTime()`/monotonic for durations. |
| Concurrency | get vs expiring delete race | Atomic conditional remove. |
| Memory | Expired keys never read | Active sweeper reclaims them. |
| Overwrite | put existing key | Replace value + reset TTL. |

---

## 5. Entities, relationships, responsibilities

- **Entry**: value + expiry timestamp (0 / absent = never).
- **Store**: the map + the TTL logic + (production) the sweeper.
- **ExpiryStrategy** (optional): pluggable lazy/active. → Strategy.

---

## 6. Class/interface design

```
KeyValueStore
   ├── Map<key, Entry(value, expiryNanos)>
   ├── get/put/delete  (lazy expiry on get)
   └── (production) background sweeper thread
```

### normal vs production
- `normal/` — `HashMap<String, Entry>`; lazy expiry on `get`; single-threaded.
- `production/` — `ConcurrentHashMap`; lazy expiry on access using **atomic conditional remove** (`remove(key, entry)` so we don't delete a freshly-overwritten value); plus a background **sweeper** thread for memory reclamation; monotonic time.

---

## Interview Questions, Follow-ups & Traps

**Q: How do you expire keys — lazy or active?** *(core)*
Both. Lazy = check on read (cheap, but unread expired keys linger). Active = a sweeper reclaims memory. Redis combines lazy with periodic random sampling. Name the trade-off and pick "both."

**Q: What's the race between get and expiry?**
`get` reads an entry, decides it's expired, and removes it — but between the read and the remove, another thread may have `put` a fresh value under the same key. A blind `map.remove(key)` would delete the fresh value. Use **conditional remove** `map.remove(key, staleEntry)` which only removes if the mapped value is still the stale one (atomic in `ConcurrentHashMap`).

**Q: Why monotonic time for expiry?**
Durations computed from wall-clock (`currentTimeMillis`) break if the system clock jumps (NTP sync). Use `System.nanoTime()` for elapsed/expiry math.

**Follow-ups**
- *"Add max size + eviction."* — Combine with LRU (see problem 09): on put when full, evict the LRU entry. Now it's a bounded TTL cache.
- *"Make the sweeper efficient at scale."* — Sample a random subset per tick (like Redis) instead of scanning everything; or keep a priority queue of expiry times.
- *"Distributed KV store?"* — Shard by key hash; replicate; TTL enforced per node; this is where it becomes a system-design question.

**Traps**
- Blind `remove(key)` on expiry (deletes a freshly-written value — the race above).
- Only lazy expiry → memory leak of unread expired keys.
- Wall-clock for TTL math.
- Returning an expired value because you forgot the expiry check on `get`.
