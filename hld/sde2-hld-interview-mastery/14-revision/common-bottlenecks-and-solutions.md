# Common Bottlenecks and Solutions

> Condensed diagnosis table. Full playbook: [scaling-bottleneck-diagnosis](../11-estimation-and-bottleneck-analysis/scaling-bottleneck-diagnosis.md). Rule: diagnose from signals (metrics→traces→logs + "what changed?"), match the fix to the bottleneck *type* — never blanket "add servers."

| Symptom / signal | Bottleneck type | Fix (matching the type) |
|---|---|---|
| Endpoint slow as data grows; DB CPU high; Seq Scan in EXPLAIN | Missing index / slow query | Add the matching composite index (cheapest rung) |
| Many tiny repeated queries per request | N+1 | Join / batch fetch (`IN`) |
| High read load; repeated reads | Read volume | Cache + read replicas |
| DB connections maxed / rejected (esp. after app scale-up) | Connection exhaustion | Smaller per-instance pools + connection pooler + replicas |
| Single primary write CPU/IO maxed; data huge | Write throughput / data size | Batch, write-optimized store, or **shard** (last resort) |
| App CPU high, DB fine | App compute | Scale app tier horizontally |
| CPU-heavy work on the request path | Work on the hot path | Move async (queue + workers) |
| Queue depth / consumer lag rising | Consumer throughput | Autoscale workers to partition count (add partitions), prioritize/shed, protect downstream |
| One cache/DB node hot, others idle | Hot key / hot shard | Local cache in front / replicate / split key; better shard key |
| p99 high, p50 fine, intermittent | Tail latency | Fix the tail cause (GC/locks/cold cache/slow dep); reduce fan-out |
| Latency up → in-flight up → pools exhaust everywhere | Cascading overload | Timeouts (p99-based) + circuit breaker + bulkhead |
| Hot key expires → DB flooded by concurrent misses | Cache stampede | Request coalescing + early refresh + TTL jitter |
| Keys absent in cache AND DB hit DB every time | Cache penetration | Negative caching + bloom filter + input validation |
| Mass expiry / cold cache restart floods DB | Cache avalanche | TTL jitter + cache warming + gradual ramp + persistence |
| Error rate spiked after a deploy | Bad deploy | **Roll back first** (mitigate), then fix root cause |
| Reads offloaded to replicas but writes still slow | Replicas don't scale writes | Batch / write-optimized store / shard writes |
| Serverless spike → DB connection exhaustion | Connection fan-out | Connection pooler/proxy between functions and DB |
| Accidental mass delete / corruption | Logical error (replicas replicated it) | Restore from backup / PITR (not replicas) |
| One slow non-critical dependency takes down the critical path | No isolation | Timeout + circuit breaker + bulkhead + graceful degradation |

## The fix ladder (DB reads, cheapest first)
**index/query → cache → read replica → shard.** Climb a rung only when the cheaper one is measured-insufficient.

## The recurring trap
"Add more app servers" to a DB-bound problem *multiplies DB connections* and worsens it. Diagnose first; fix the actual bottleneck.

## The scaling-dimension reminder
Users ≠ RPS ≠ data size ≠ connections ≠ throughput ≠ geography. Scale the dimension actually under pressure (re-estimate before acting).

---
**Next:** [common-follow-up-questions](./common-follow-up-questions.md)
