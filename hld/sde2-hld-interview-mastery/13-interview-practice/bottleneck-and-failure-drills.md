# Bottleneck and Failure Drills

> Related: [11-estimation-and-bottleneck-analysis/scaling-bottleneck-diagnosis](../11-estimation-and-bottleneck-analysis/scaling-bottleneck-diagnosis.md) · [09-observability-and-operations/incident-response-and-root-cause-analysis](../09-observability-and-operations/incident-response-and-root-cause-analysis.md)

Scenario drills: "the system is slow / failing — diagnose it." These train the diagnose-from-signals skill (not "add servers"), which is a top SDE-2 discriminator.

---

## The diagnosis pattern

> Signal (metrics → traces → logs) + "what changed?" → classify the bottleneck *type* → apply the matching fix. (See [scaling-bottleneck-diagnosis](../11-estimation-and-bottleneck-analysis/scaling-bottleneck-diagnosis.md).)

---

## Drills — for each symptom, name the likely bottleneck, the signal to check, and the fix. Solutions below.

1. One endpoint got slow as data grew; DB CPU high.
2. You added app servers and it got *slower*.
3. DB rejecting connections after a scale-up.
4. p99 doubled but p50 is fine, intermittently.
5. Notifications delayed; queue depth rising.
6. One cache node is hot while others idle.
7. A dependency slowed and the whole API fell over.
8. After a deploy, error rate spiked.
9. Cache restarted and the DB fell over.
10. Writes are the bottleneck even after adding read replicas.

---

## Solutions

1. **Missing index / slow query.** Signal: EXPLAIN (Seq Scan), slow-query log. Fix: add the matching index (cheapest rung) — not sharding, not more servers. (See [indexing-and-query-optimization](../03-databases-and-data-modeling/indexing-and-query-optimization.md).)
2. **DB was the bottleneck, not the app.** Signal: DB connections/CPU maxed while app CPU fine. Fix: connection pooler + cache + replicas; stop scaling the app (it multiplies DB connections). (See [horizontal-vs-vertical-scaling](../02-compute-and-architecture/horizontal-vs-vertical-scaling.md).)
3. **Connection exhaustion.** Signal: DB connection count = max, rejections. Fix: smaller per-instance pools + a connection pooler (multiplex many→few). (See [database-capacity-and-connection-pools](../11-estimation-and-bottleneck-analysis/database-capacity-and-connection-pools.md).)
4. **Tail-latency cause.** Signal: traces show a slow span on some requests. Likely GC pauses, lock contention, cold cache, or a slow dependency. Fix: fix the tail cause / reduce fan-out — not average optimization. (See [latency-budgets-and-tail-latency](../11-estimation-and-bottleneck-analysis/latency-budgets-and-tail-latency.md).)
5. **Consumer lag (throughput bottleneck).** Signal: consumer-lag metric rising. Fix: autoscale workers to partition count (add partitions), prioritize/shed, protect downstream during recovery. (See [backpressure-and-consumer-lag](../05-messaging-and-event-driven-systems/backpressure-and-consumer-lag.md).)
6. **Hot key.** Signal: per-node load skew. Fix: local cache in front of the hot key / replicate / split the key. (See [stampedes-hot-keys-and-penetration](../04-caching/stampedes-hot-keys-and-penetration.md).)
7. **Cascading overload (no timeout).** Signal: in-flight requests + latency climbing, thread/connection pool saturation. Fix: timeouts (p99-based) + circuit breaker + bulkhead. (See [partial-failures-and-timeouts](../06-distributed-systems/partial-failures-and-timeouts.md).)
8. **Bad deploy.** Signal: "what changed?" = the deploy; traces/logs show the new fault (e.g., a missing-index query). Fix: **roll back first** (mitigate), then fix the root cause. (See [incident-response-and-root-cause-analysis](../09-observability-and-operations/incident-response-and-root-cause-analysis.md).)
9. **Cache avalanche (cold cache).** Signal: DB load spike after cache restart. Fix: warm hot keys, ramp with a DB concurrency limiter, TTL jitter, cache persistence/replication. (See [stampedes-hot-keys-and-penetration](../04-caching/stampedes-hot-keys-and-penetration.md), [redis-design-and-failure-modes](../04-caching/redis-design-and-failure-modes.md).)
10. **Replicas scale reads, not writes.** Signal: primary write CPU/IO maxed. Fix: batching, write-optimized store, or shard writes — replicas don't help writes. (See [replication-and-read-replicas](../03-databases-and-data-modeling/replication-and-read-replicas.md).)

---

## Failure-mode drills — "what happens when X fails, and how do you handle it?"

- **Redis down** → cache reads fall back to the DB behind a concurrency limiter + tight timeouts; limiter fails open with a local cap; sessions on JWT survive. (See [redis-design-and-failure-modes](../04-caching/redis-design-and-failure-modes.md).)
- **A worker crashes after a side effect, before ack** → at-least-once redelivery → idempotent processing makes the re-run safe. (See [idempotency-and-deduplication](../05-messaging-and-event-driven-systems/idempotency-and-deduplication.md).)
- **DB primary dies** → quorum-coordinated replica promotion with fencing (no split-brain); data loss bounded by replication lag (RPO). (See [redundancy-and-failover](../07-reliability-and-resilience/redundancy-and-failover.md).)
- **An AZ fails** → N+1/N+2 headroom + multi-AZ redundancy absorb it; survivors must not be overloaded. (See [availability-and-reliability](../07-reliability-and-resilience/availability-and-reliability.md).)
- **A non-critical dependency fails** → graceful degradation (omit the feature, serve the core) + circuit breaker + bulkhead. (See [graceful-degradation](../06-distributed-systems/graceful-degradation.md).)
- **Accidental mass delete / corruption** → restore from backup / PITR (replicas don't help — they replicated it). (See [backups-and-point-in-time-recovery](../07-reliability-and-resilience/backups-and-point-in-time-recovery.md).)
- **Provider charged but our write failed** → webhook + reconciliation catch it; idempotency key prevents a double-charge on retry. (See [idempotent-recovery-and-reconciliation](../07-reliability-and-resilience/idempotent-recovery-and-reconciliation.md).)

---

## Exercise

Have someone give you a vague "the system is slow" or "X is failing" for a design you did. Diagnose from signals (metrics→traces→logs + "what changed?"), classify the bottleneck type, and give the matching fix — out loud, in under 3 minutes. Resist "add more servers."

**Next:** [scaling-followups](./scaling-followups.md)
