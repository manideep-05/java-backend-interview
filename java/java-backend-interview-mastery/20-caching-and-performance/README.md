# 20 — Caching & Performance

> **Priority: P1.** The signal is reasoning about **correctness**, not just speed: invalidation, staleness, stampede, and when caching makes a system *worse*. "There are only two hard things… cache invalidation" is a real interview theme.

**Prerequisites:** [03 Collections](../03-collections-framework/), [17 DB transactions](../17-database-transactions-and-isolation/).
**Related:** [07 JVM](../07-jvm-memory-and-performance/) (cache = leak risk), [16 Indexing](../16-database-indexing-and-query-optimization/).

---

## A. Concept & intuition
A cache stores the result of expensive work (a query, a computation, a remote call) so repeated requests are cheaper — reducing latency, load, and cost. The fundamental tension: a cache is a **copy**, and copies go stale. Every caching decision is really a correctness decision about how stale you can tolerate and how you invalidate.

## B. Core concepts

### Local vs distributed
- **Local (in-process, e.g., Caffeine):** fastest (no network), but per-instance (each node has its own copy → inconsistency across nodes) and bounded by heap.
- **Distributed (e.g., Redis):** shared across instances (consistent view, survives restarts), but adds network latency, serialization, and an operational dependency.

### Patterns
- **Cache-aside (lazy):** app checks cache; on miss, loads from DB and populates. Most common; the app owns invalidation.
- **Write-through:** writes go to cache and DB synchronously (cache always fresh, slower writes).
- **Write-behind:** writes to cache, async to DB (fast, risk of loss/inconsistency).

### TTL & eviction
TTL bounds staleness and memory. Eviction policies (LRU/LFU/size) bound memory when full. A cache without a bound/TTL is a **memory leak** ([07](../07-jvm-memory-and-performance/)).

### Invalidation (the hard part)
On a write, the cached copy is stale. Options: evict the key on write (simple, a small race window), update the cache on write (write-through), or rely on TTL (eventual). **Ordering matters:** update the DB *then* invalidate the cache; invalidating first then writing can repopulate stale data from a concurrent read.

### Stampede / thundering herd
When a hot key expires, many requests miss simultaneously and all hit the DB at once. Fixes: **single-flight** (one loader computes, others wait), slightly randomized TTLs (jitter) to desynchronize expiry, or refresh-ahead.

### Penetration & hot keys & negative caching
- **Penetration:** requests for keys that don't exist bypass the cache and hammer the DB → **negative caching** (cache the "not found" briefly).
- **Hot key:** one key gets disproportionate traffic → local-cache it in front of the distributed cache.

### Serialization & key design
Cache keys must be deterministic and collision-free (include all inputs/version). Serialization cost (and size) can erase the benefit for large/complex objects.

### When caching hurts
Low hit rate (keys rarely reused) adds overhead with no benefit; cached mutable/sensitive data causes correctness/security bugs; an extra network hop to a remote cache can be slower than a fast indexed query; cache failures can take down the app if there's no fallback.

### Redis depth (the distributed cache you'll be asked about)
Redis comes up whenever you list it on a resume, so know more than "it's a cache":
- **Single-threaded command execution:** Redis runs commands on one thread (an event loop), so each command is atomic and there are no data races between commands — but one slow command (e.g., `KEYS *`, a big `SORT`) blocks everything. Use `SCAN` instead of `KEYS`, and avoid O(N) commands on huge keys.
- **Data structures (not just key→value):** strings, hashes (an object's fields), lists (queues), sets, sorted sets (`ZSET` — leaderboards, sliding-window rate limiters, time-ordered data), bitmaps, HyperLogLog (approximate cardinality), streams (durable log + consumer groups). Choosing the right structure is the main Redis design question.
- **Persistence:** **RDB** (periodic snapshots — compact, can lose the last few minutes on crash) vs **AOF** (append-only write log — more durable, larger, replayed on restart); you can run both. Pure cache → persistence optional; datastore → AOF. Durability-vs-performance trade-off.
- **Eviction:** at `maxmemory`, evict per policy — `allkeys-lru`/`allkeys-lfu` (cache), `volatile-ttl` (only keys with a TTL), or `noeviction` (writes fail). Be deliberate about which keys can be evicted when mixing cache + durable data.
- **Expiry:** TTL via `EX`/`PX`; keys expire lazily (on access) + a periodic sampler, so an expired key can linger briefly — relevant for memory sizing, not correctness.
- **Pipelining & atomic ops:** pipeline commands to avoid per-command round trips; Lua scripts / `MULTI`/`EXEC` give atomic multi-step server-side operations.
- **Pub/Sub vs Streams:** `PUB/SUB` is fire-and-forget (a disconnected subscriber misses messages); **Streams** are durable with consumer groups → use Streams when you need at-least-once delivery.
- **Distributed locks:** `SET key val NX PX <ttl>` is a simple lock with a safety TTL. **Redlock** (multi-node) has known caveats under clock skew / GC pauses — for critical correctness prefer a DB constraint / fencing token over "a Redis lock guarantees exclusivity." Saying this nuance signals seniority.
- **Replication & cluster:** replicas scale reads but replication is **asynchronous** → possible stale reads and lost recent writes on failover; cluster shards by hash slot and restricts cross-slot multi-key ops. Don't treat Redis as strongly consistent.
- **Redis vs Caffeine:** Caffeine (in-process) for the hottest, read-mostly, per-node-tolerable data (nanosecond access, no network, but per-node divergence, heap-bound); Redis when instances must share state, data must survive restarts, or you need its structures (rate limiting, leaderboards, locks, queues). Common pattern: a small Caffeine layer in front of Redis for hot keys.

## C. Code — cache-aside with correct invalidation (Spring Cache)
```java
@Service
class DocumentService {
    @Cacheable(cacheNames = "docMeta", key = "#id")        // populate on miss
    public DocMeta getMeta(Long id) { return repo.findMeta(id); }

    @Transactional
    @CacheEvict(cacheNames = "docMeta", key = "#id")        // evict AFTER the DB write (see note)
    public void updateMeta(Long id, DocMeta meta) {
        repo.update(id, meta);
        // Spring evicts after the method returns (post-commit if configured) -> DB-then-cache ordering
    }
}
```
```java
// Caffeine config: BOUNDED + TTL + jittered refresh to avoid stampede/leak
Caffeine.newBuilder()
    .maximumSize(10_000)                         // bound memory -> no leak
    .expireAfterWrite(Duration.ofMinutes(5))     // bound staleness
    .refreshAfterWrite(Duration.ofMinutes(4));   // refresh-ahead reduces stampede
```
Note: for strict correctness under concurrency, evict after commit (so a concurrent read can't repopulate pre-commit data). Prefer eviction over in-place update to avoid caching uncommitted state.

## D. Trade-offs

| Choice | Pros | Cons | When |
|---|---|---|---|
| Local (Caffeine) | fastest, no network | per-node inconsistency, heap-bound | hot read-mostly data, per-node OK |
| Distributed (Redis) | shared, survives restart | network/serialization, dependency | cross-instance consistency |
| Cache-aside | simple, app-controlled | miss penalty, invalidation on app | general reads |
| Write-through | always fresh | slower writes | read-after-write consistency |
| Write-behind | fast writes | loss/inconsistency risk | high write throughput, tolerant |
| TTL-only | simple | stale window | tolerant of eventual freshness |

## E. Interview Q&A

### CACHE-01 (P1) — Cache-aside vs write-through vs write-behind?
**Strong answer:** Cache-aside: the app reads cache, loads from DB on miss, and invalidates on write — simple and common, but there's a miss penalty and invalidation is the app's job. Write-through writes to cache and DB together so reads are always fresh at the cost of slower writes. Write-behind writes to cache and asynchronously to the DB for fast writes, risking loss/inconsistency on crash. I default to cache-aside with careful invalidation.

### CACHE-02 (P1) — Local vs distributed cache?
**Strong answer:** Local (Caffeine) is fastest with no network hop but each instance has its own copy, so values can diverge across nodes and it's bounded by heap. Distributed (Redis) gives a shared, consistent, restart-surviving cache at the cost of network latency, serialization, and an extra dependency. I often combine them: a small local cache in front of Redis for hot keys.

### CACHE-03 (P1) — What is a cache stampede and how do you prevent it?
**Strong answer:** When a hot key expires, many concurrent requests all miss and hit the backend at once, spiking load. I prevent it with single-flight (one request loads while others wait for the result), TTL jitter so keys don't expire together, and refresh-ahead to renew before expiry. It's the caching cousin of a retry storm.

<a id="cache-04"></a>
### CACHE-04 (P1) — How can caching introduce correctness bugs?
**Strong answer:** A cache is a copy that goes stale: if I invalidate before committing the DB write (or in the wrong order), a concurrent read can repopulate the cache with old data that then persists; TTL-only caches serve stale data within the window; per-node local caches diverge across instances; caching mutable or security-sensitive data can expose outdated or wrong values. The fixes are DB-then-cache ordering (evict after commit), preferring eviction over caching uncommitted state, bounding staleness with TTL, and not caching data that must be strongly consistent. I treat every cache as an eventual-consistency decision.
**Weak answer:** "Caching just makes it faster." Ignores the staleness/consistency cost that causes real bugs.

### CACHE-05 (P1) — When does caching make things worse?
**Strong answer:** When the hit rate is low (keys rarely reused), the cache adds lookup/serialization overhead and memory for no benefit; when the cached object is large, serialization can cost more than recomputing; when a remote cache hop is slower than a well-indexed query; when an unbounded cache leaks memory; and when a cache outage has no fallback and takes the app down. I measure hit rate and latency before and after, and ensure a cache miss/failure degrades gracefully to the source.

### CACHE-06 (P2) — Cache penetration and negative caching?
**Strong answer:** Penetration is repeated requests for keys that don't exist, which miss the cache and hammer the DB (sometimes maliciously). Negative caching stores the "not found" result for a short TTL so repeated misses are absorbed; I keep the negative TTL short and may add a bloom filter for large key spaces.

### CACHE-07 (P1) — Redis vs Caffeine, and what do you need to know about Redis itself?
**Strong answer:** Caffeine is an in-process cache — nanosecond access, no network, ideal for the hottest read-mostly data a node can hold, but each instance has its own copy (divergence) and it's heap-bound. Redis is a shared, out-of-process store I use when multiple instances must see the same data, data must survive a restart, or I need its data structures. On Redis specifically: it executes commands single-threaded so each command is atomic but a slow command (`KEYS *`) blocks everything (use `SCAN`); it offers rich structures (sorted sets for leaderboards/rate limiting, streams for durable messaging); persistence is RDB snapshots vs AOF write-log (durability vs performance); eviction policies like `allkeys-lru` apply at `maxmemory`; replication is asynchronous so replicas can be stale and a failover can lose recent writes — so I don't treat it as strongly consistent. I often layer a small Caffeine cache in front of Redis for hot keys.
**Follow-up:** "Is a Redis distributed lock safe?" (`SET NX PX` is a simple lock; Redlock has clock-skew/GC caveats — for critical correctness I back it with a DB constraint / fencing token, not Redis alone.)

### CACHE-08 (P2) — When would you NOT use Redis even though it's available?
**Strong answer:** When a well-indexed DB query is already fast enough (the network hop to Redis can be slower), when the data must be strongly consistent (async replication means stale reads/lost writes on failover), when the dataset is tiny and per-node (Caffeine is simpler), or when adding Redis means a new operational dependency and failure mode without a real hit-rate/latency win. I measure before adding it.

## F. Production relevance
Caching is a top performance lever and a top source of subtle bugs. The incidents: stale metadata after an update (wrong invalidation ordering), memory growth from an unbounded cache ([DBG-03](../22-debugging-and-production-engineering/)), a stampede melting the DB when a hot key expires, and cross-node inconsistency from local caches. The disciplined approach: bound size + TTL, evict after commit (DB-then-cache), single-flight/jitter for hot keys, graceful fallback on cache failure, don't cache data that must be strongly consistent, and verify hit rate justifies the cache. Correctness first, speed second.

## G. Revision

### 10 key takeaways
1. A cache is a copy; every decision is about staleness tolerance + invalidation.
2. Local (fast, per-node divergent) vs distributed (shared, network cost).
3. Cache-aside is the common default; write-through for freshness; write-behind risks loss.
4. Always bound size + TTL (unbounded = memory leak).
5. Invalidate in DB-then-cache order; evict after commit.
6. Prefer eviction over caching uncommitted state.
7. Stampede → single-flight + TTL jitter + refresh-ahead.
8. Negative-cache missing keys to stop penetration.
9. Low hit rate / big objects / remote hop can make caching slower.
10. Fail gracefully to the source on cache miss/outage.

### 10 rapid-fire
1. Most common pattern? (cache-aside) 2. Always-fresh pattern? (write-through) 3. Unbounded cache = ? (leak) 4. Hot-key expiry problem? (stampede) 5. Fix stampede? (single-flight/jitter) 6. Missing-key floods? (negative cache) 7. Local cache cross-node? (divergent) 8. Invalidate order? (DB then cache) 9. Cache uncommitted? (no) 10. Low hit rate? (cache hurts)

### 5 tricky
1. Why did metadata stay stale after an update? (invalidation order/race) 2. Why did a hot key melt the DB? (stampede) 3. Why did adding a cache slow reads? (low hit rate/remote hop) 4. Why did the app crash with the cache? (unbounded leak / no fallback) 5. Why evict after commit?

### 5 exercises
1. Implement cache-aside with Caffeine (bounded + TTL) and correct eviction.
2. Reproduce a stampede, then fix with refresh-ahead/jitter.
3. Add negative caching for missing keys.
4. Show stale-read from invalidating before commit; fix with after-commit eviction.
5. Measure hit rate and latency to decide if a cache is worth it.

### One-page revision sheet
> Cache = copy → staleness + invalidation decision. **Local(fast, per-node divergent) vs distributed(shared, network).** Cache-aside(default)/write-through(fresh)/write-behind(fast, risky). **Always bound size+TTL** (else leak). **Invalidate DB-then-cache, evict after commit**; don't cache uncommitted. Stampede → single-flight + TTL jitter + refresh-ahead. Negative-cache missing keys (penetration). Caching hurts when: low hit rate, big objects, remote hop slower than indexed query, no fallback. Correctness > speed.
> **Redis:** single-threaded (atomic commands, `KEYS *` blocks → use `SCAN`); structures (ZSET=leaderboard/rate-limit, streams=durable msg); persistence RDB(snapshot) vs AOF(log); eviction allkeys-lru at maxmemory; **async replication → stale reads / lost writes on failover (not strongly consistent)**; `SET NX PX` lock, Redlock has clock/GC caveats → back critical locks with a DB constraint. Caffeine(in-process, hot, per-node) vs Redis(shared, durable, structures); layer both.

### Prerequisites to revisit
- Collections/LRU (ch.03); memory leaks (ch.07); consistency/commit timing (ch.13, 17).
