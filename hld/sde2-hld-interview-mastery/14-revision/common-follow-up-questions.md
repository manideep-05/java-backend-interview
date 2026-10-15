# Common Follow-up Questions (Rapid Answers)

> One-line answers to the follow-ups that come up in almost every interview. Full treatments in [13-interview-practice](../13-interview-practice/mock-interviews.md) and the concept chapters. Drill these until automatic.

## Scaling
- **10× traffic?** → Re-estimate the pressured dimension; name first bottleneck + matching fix, then the next. Not "scale horizontally."
- **First bottleneck?** → Usually the DB (reads in read-heavy systems); name how you'd detect it.
- **Why not add more servers?** → Scales the app but multiplies DB connections → worsens a DB bottleneck.
- **When to shard?** → Only when a single primary's writes/data size is the measured ceiling (after index/cache/replica).
- **Hot partition?** → Better/compound shard key, split the hot key, cache it, dedicated capacity.
- **100M users — redesign?** → MAU ≠ RPS; re-estimate; maybe no change.

## Databases
- **SQL vs NoSQL?** → SQL for relationships/transactions/evolving queries; NoSQL for a known access pattern at scale.
- **Redis as primary DB?** → No — it's a cache (lossy, in-memory); the source of truth needs durability/queries.
- **Replica falls behind?** → Replication lag → stale reads; route read-your-write to the primary; alert on lag.
- **Migrate a huge table?** → Expand → migrate (batched backfill) → contract; backward-compatible; online schema tools.
- **Prevent overselling?** → Atomic conditional decrement (`WHERE qty>0`); never read-check-then-write.

## Caching
- **Redis down?** → Fall back to DB behind a concurrency limiter + tight timeouts; limiter fails open with a local cap.
- **Prevent a stampede?** → Request coalescing (single-flight) + early refresh + TTL jitter.
- **Invalidate cached data?** → TTL (self-healing) + explicit delete-on-write + CDC for robustness.
- **One key gets most traffic?** → Hot key: local cache in front / replicate / split the key.
- **Serve stale data safely?** → For change-tolerant data yes (bounded TTL); never for strongly-consistent data (balance/authz).

## Messaging
- **Consumer crashes after processing?** → At-least-once redelivery → idempotent processing makes the re-run safe.
- **Prevent duplicate business operations?** → Idempotency key, atomic check-and-record before the effect.
- **Poison messages?** → Retry limit → DLQ + alert; avoids head-of-line blocking.
- **Consumer lag?** → Autoscale to partition count (add partitions), prioritize/shed, protect downstream in recovery.
- **Preserve ordering?** → Partition by the ordering key (per-key ordering); global ordering kills parallelism.

## Distributed systems
- **Guarantee exactly-once?** → Not delivery (impossible); effectively-once = at-least-once + idempotency (+ reconciliation).
- **Handle partial failures?** → Timeouts (p99), idempotent retries + backoff/jitter, circuit breakers, degradation.
- **Prevent duplicate payments?** → Idempotency key (atomic, passed to the provider) + reconciliation.
- **Two servers own the same lock?** → GC pause past TTL → fencing tokens (resource rejects stale holder); or avoid the lock (idempotency).
- **What consistency does it need?** → Per path: strong for money/inventory, eventual for feeds/counts, read-your-write for user-visible writes.

## Reliability
- **AZ fails?** → Multi-AZ redundancy + N+1/N+2 headroom; LB routes around it; survivors must not be overloaded.
- **Recover from corruption?** → Backup / PITR (replicas replicated the corruption); replicas don't help logical errors.
- **External dependency outage?** → Timeout + circuit breaker + fallback (graceful degradation); make it async if possible.
- **Active-active trade-offs?** → Low global latency + seamless failover, but write conflicts + complexity + cost.
- **Measure an SLO?** → Pick user-facing SLIs (p99, success rate), set a target + error budget, alert on burn rate.

## Architecture
- **Microservices vs monolith?** → Modular monolith default; decompose on team/scaling pressure (not for "scalability").
- **When an API gateway?** → Multiple services needing centralized authn/rate-limit/routing (not a single service).
- **Downside of Kafka?** → Ops complexity + duplicates (idempotency) + overkill if no replay/multi-consumer need.
- **Queue grows indefinitely?** → Backpressure/shed + autoscale consumers; a persistent producer>consumer gap overflows retention (loss).
- **Reduce cost without losing guarantees?** → Cache (fewer DB reads), storage tiering/retention, right-size pools/instances, sample loss-tolerant data.

## The two phrases that win follow-ups
1. "First, which dimension does that pressure?" (forces re-estimation)
2. "The first bottleneck becomes X; after I fix it, the next is Y." (shows evolution, not one-shot scaling)

---
**Next:** [final-interview-revision](./final-interview-revision.md)
