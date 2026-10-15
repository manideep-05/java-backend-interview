# Cache Sizing and Hit Ratio

> Prerequisites: [04-caching/caching-fundamentals](../04-caching/caching-fundamentals.md), [estimation-cheat-sheet](./estimation-cheat-sheet.md)
> Related: [04-caching/ttl-eviction-and-memory-management](../04-caching/ttl-eviction-and-memory-management.md)

Sizing a cache to the working set (not the whole dataset) and estimating the hit ratio and resulting DB offload — so you can justify cache size and prove the DB-load reduction.

---

## A. Formulas

- **Cache memory to hold the working set** = `working-set entries × entry size`.
- **DB read RPS after caching** = `total read RPS × (1 − hit ratio)`.
- **DB-load reduction factor** = `1 / (1 − hit ratio)` (95% hit → 20× reduction).
- **Working set** ≈ the hot subset actually accessed (often ~20% of data per the 80/20 heuristic captures ~80% of reads).

The key idea: you size to the *working set* (hot data), not the full dataset — a small cache captures most reads.

---

## B. Worked example — DMS metadata cache

100M documents, metadata entry ~500 bytes, 10,000 read RPS, 80/20 access pattern:
- **Cache everything:** 100M × 500B = **50 GB** (large, mostly cold data wasted).
- **Cache the working set (hot 20%):** 20M × 500B = **10 GB** — captures ~80%+ of reads at 1/5 the memory.
- **Hit ratio ~85%** (hot set + some TTL misses): DB read RPS = 10,000 × 0.15 = **~1,500** (down from 10,000 — a ~6–7× reduction).
- **At 95% hit** (bigger/warmer cache): DB reads = 10,000 × 0.05 = **~500** (20× reduction).

**Decision:** cache the ~10 GB working set (one Redis node), accept ~85–95% hit ratio, cutting DB reads several-fold — justifies the cache size and quantifies the offload. See [caching-fundamentals](../04-caching/caching-fundamentals.md).

---

## C. Key insights

- **Size to the working set, not the dataset** — caching cold data wastes memory for little hit-ratio gain (diminishing returns past the hot set).
- **Hit ratio drives the DB offload** — the relationship is non-linear: 90%→95% hit ratio *halves* DB reads again (10% → 5% miss). Pushing hit ratio high has outsized DB benefit.
- **TTL and eviction affect hit ratio** — short TTLs lower hit ratio (more reloads); LRU keeps the hot set (see [ttl-eviction-and-memory-management](../04-caching/ttl-eviction-and-memory-management.md)).
- **Measure, don't guess** — real access patterns (from logs) tell you the working-set size and achievable hit ratio; the 80/20 rule is a starting heuristic.

---

## D. Interview Q&A

- Q: "You have 10,000 read RPS and a 95% cache hit ratio. DB read load?" → *10,000 × 5% = ~500 DB read RPS — a 20× reduction, which is why caching is the top read-scaling lever.*
- Q: "Should you cache all 100M entries?" → *No — cache the working set (hot ~20%), which captures most reads at a fraction of the memory. Caching cold data has diminishing returns.*
- Q: "How much does improving hit ratio from 90% to 95% help?" → *It halves DB reads (10%→5% miss) — a big, non-linear win, so pushing hit ratio high is worthwhile for a DB-bound system.*

**Common wrong answers:** sizing the cache for the full dataset; guessing hit ratio instead of measuring; ignoring that hit-ratio gains are non-linear in DB offload.

---

## E. Exercise

A product-catalog service: 50M products, cache entry ~1KB, 20,000 read RPS, 70/30 access skew (30% of products get 70% of reads). Compute: (a) memory to cache all, (b) memory for the hot 30%, (c) DB read RPS if the hot-set cache gives ~75% hit ratio, and (d) DB read RPS if you grow it to 95%. Recommend a cache size and justify.
*(Answer: (a) 50M×1KB = 50 GB; (b) 15M×1KB = 15 GB; (c) 20,000×0.25 = 5,000 DB RPS; (d) 20,000×0.05 = 1,000 DB RPS. Recommend caching the ~15 GB hot set (one node), tuning TTL/eviction toward a high hit ratio since 95% cuts DB reads 20× — measure the real working set from access logs.)*

---

## F. Revision summary

**Key takeaways**
- Size the cache to the working set (hot data), not the full dataset — a small cache captures most reads (80/20).
- DB read RPS after caching = total reads × (1 − hit ratio); offload factor = 1/(1−hit ratio) (95% → 20×).
- Hit-ratio gains are non-linear in DB offload — pushing hit ratio high is worthwhile.
- Measure the real working set and achievable hit ratio; TTL/eviction tune it.

**Rapid-fire**
1. Size the cache to? → The working set (hot data), not the whole dataset.
2. DB reads after caching = ? → total reads × (1 − hit ratio).
3. 95% hit ratio reduces DB reads by? → 20×.
4. 90%→95% hit ratio does what to DB reads? → Halves them (non-linear).
5. How to find the working-set size? → Measure real access patterns.

**Next:** [latency-budgets-and-tail-latency](./latency-budgets-and-tail-latency.md)
