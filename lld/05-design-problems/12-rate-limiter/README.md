# Rate Limiter — LLD

Tests whether you know the standard algorithms and can reason about atomic counters under concurrency.

---

## 1. Requirements

**Functional**
- `allow(clientId)` → boolean: is this request permitted *right now*?
- A limit like "N requests per T seconds" per client.
- Pluggable algorithm (fixed window, sliding window, token bucket).

**Non-functional**
- Called on the hot path of every request → must be fast and thread-safe.
- Per-client state (so one client can't starve others).

---

## 2. The algorithms (know the trade-offs)

| Algorithm | Idea | Pro | Con |
|-----------|------|-----|-----|
| **Fixed window** | Count requests in the current T-second bucket. | Trivial, O(1) memory. | **Burst at edges**: 2×N requests across a window boundary. |
| **Sliding window log** | Keep timestamps; count those within the last T. | Accurate. | O(requests) memory. |
| **Sliding window counter** | Weighted blend of current + previous window. | Smooths edges, cheap. | Approximate. |
| **Token bucket** | Tokens refill at a steady rate; each request spends one. | Allows controlled bursts; smooth. | Slightly more state. |

Token bucket and sliding-window-counter are the usual "good" answers.

---

## 3. Critical flow

`allow(clientId)`:
1. Load (or create) this client's limiter state.
2. Refill/advance based on elapsed time.
3. If capacity/tokens remain → consume one, return `true`; else return `false`.

Steps 2–3 are a **check-then-act** on shared counters → must be atomic.

---

## 4. Edge cases & how to spot them

| Source | Edge | Handling |
|--------|------|----------|
| Concurrency | Two requests hit the last token together | Atomic decrement/CAS; only one succeeds. |
| Time | Clock skew / window rollover | Use monotonic time (`System.nanoTime`); recompute refill from elapsed. |
| Cold start | First request for a new client | Lazily create full-capacity state. |
| Boundary | Exactly at the limit | Define `<` vs `<=` precisely. |
| Memory | Millions of idle clients | Evict stale client state (LRU/TTL). |

---

## 5. Entities, relationships, responsibilities

- **RateLimiter** (interface): `allow(clientId)`. → **Strategy** seam for the algorithm.
- **TokenBucketRateLimiter** / **FixedWindowRateLimiter**: concrete algorithms.
- **Per-client state** (e.g. `Bucket` with tokens + lastRefill): one per client, stored in a map.
- *Responsibility split:* the limiter decides policy; the bucket holds counters; a registry maps client → bucket.

---

## 6. Class/interface design

```
RateLimiter (interface)   <- Strategy
   ^                 ^
TokenBucket     FixedWindow
   |  holds Map<clientId, Bucket/Window>
```

### normal vs production
- `normal/` — single-threaded token bucket; plain fields; shows the refill math clearly.
- `production/` — per-client `Bucket` guarded so refill+consume is atomic; client map is a `ConcurrentHashMap`; uses `System.nanoTime()` for monotonic time. We discuss `synchronized` on the bucket vs a fully lock-free CAS loop.

---

## Interview Questions, Follow-ups & Traps

**Q: Fixed window's flaw?** *(classic trap)*
Edge bursting: with limit 100/min, a client can send 100 at 11:00:59 and 100 at 11:01:00 — 200 in ~1 second — because the counter resets at the boundary. Sliding window or token bucket fixes this.

**Q: How does token bucket allow bursts but still limit average rate?**
The bucket holds up to `capacity` tokens and refills at `rate` tokens/sec. A client can burst up to `capacity` instantly, but sustained throughput is capped at the refill rate.

**Q: Why is `allow()` a check-then-act hazard?**
"Are there tokens? → spend one" is two steps. Concurrently, two threads could both see one token and both spend it. Make it atomic (synchronized bucket, or CAS loop on an atomic token count).

**Follow-ups**
- *"Make it distributed across many servers."* — Move state to Redis; use atomic Lua scripts (`INCR`+`EXPIRE`, or a token-bucket script) so the check-then-act is atomic across the cluster.
- *"How do you refill lazily?"* — On each call compute `tokens = min(capacity, tokens + elapsed * rate)`; no background thread needed.
- *"Memory for huge client counts?"* — Evict idle buckets via TTL/LRU.

**Traps**
- Choosing fixed window without mentioning edge bursts.
- Non-atomic token consumption (the race above).
- Using wall-clock (`currentTimeMillis`) where it can jump; prefer monotonic `nanoTime` for elapsed.
- A single global lock serializing all clients (shard per client instead).
