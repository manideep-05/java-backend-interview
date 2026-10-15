# Common Architectural Mistakes

> Related: [architecture-defense-questions](./architecture-defense-questions.md) · [common-interviewer-pushbacks](./common-interviewer-pushbacks.md)

A catalog of the mistakes that sink SDE-2 interviews (and real systems). For each: the bad design, why it fails, and the better alternative. Most reduce to two meta-mistakes: **over-engineering** (complexity without a justifying requirement) and **happy-path thinking** (ignoring failure/concurrency).

---

## 1. Microservices before requirements
**Bad:** Start with 10 microservices for a small team/product. **Why it fails:** network calls, distributed transactions, and ops overhead with no team/scaling pressure to justify them → slow progress, distributed-monolith risk. **Better:** modular monolith; extract services on measured need. (See [monolith-vs-microservices](../02-compute-and-architecture/monolith-vs-modular-monolith-vs-microservices.md).)

## 2. Kafka (or any queue) when a sync call suffices
**Bad:** Add Kafka to "decouple" a fast, needed-now operation. **Why it fails:** adds latency, duplicates (idempotency), ordering concerns, and ops for no async requirement. **Better:** sync call; add a queue only for slow/heavy/unreliable work or real decoupling. (See [synchronous-vs-asynchronous-processing](../02-compute-and-architecture/synchronous-vs-asynchronous-processing.md).)

## 3. Redis without an invalidation strategy
**Bad:** "Add a cache" with no plan for when/how it's invalidated. **Why it fails:** stale data served indefinitely; stale permissions = a security bug. **Better:** define TTL + explicit/CDC invalidation per data type, and a Redis-down fallback. (See [cache-invalidation-and-consistency](../04-caching/cache-invalidation-and-consistency.md).)

## 4. NoSQL without knowing access patterns
**Bad:** "Use MongoDB for flexibility," then need joins/transactions. **Why it fails:** app-side joins, weak transactions, data anomalies. **Better:** model access patterns first; usually relational; NoSQL for a known pattern at scale. (See [relational-vs-nosql](../03-databases-and-data-modeling/relational-vs-nosql.md).)

## 5. Treating load balancing as "scaling solved"
**Bad:** "Add a load balancer and more app servers" for a DB-bound problem. **Why it fails:** more app instances multiply DB connections → *worsen* the real bottleneck. **Better:** diagnose the bottleneck; fix the DB (index/cache/replica/shard). (See [horizontal-vs-vertical-scaling](../02-compute-and-architecture/horizontal-vs-vertical-scaling.md).)

## 6. Retrying every failure indefinitely
**Bad:** Immediate, unbounded retries on any failure. **Why it fails:** retry storms (amplify an outage); retrying non-idempotent ops double-charges; retrying 4xx wastes capacity. **Better:** exponential backoff + jitter, bounded budget, retry only idempotent + retriable (5xx/timeout) ops, + circuit breaker + DLQ. (See [retries-backoff-and-jitter](../05-messaging-and-event-driven-systems/retries-backoff-and-jitter.md).)

## 7. Assuming a distributed lock guarantees correctness
**Bad:** "A Redis lock ensures only one worker runs this." **Why it fails:** a GC pause past the lock TTL leaves two holders → corruption. **Better:** fencing tokens (resource rejects stale holders), or avoid the lock via idempotency/partitioning. (See [distributed-locks](../06-distributed-systems/distributed-locks.md).)

## 8. Assuming "exactly-once delivery" eliminates duplicate side effects
**Bad:** "Kafka exactly-once, so no double-charge." **Why it fails:** EOS is Kafka-internal; your external effect (charge/email) still duplicates on retry. **Better:** idempotency at the side-effect boundary + reconciliation → effectively-once. (See [exactly-once-processing](../06-distributed-systems/exactly-once-processing.md).)

## 9. Ignoring database connection limits
**Bad:** Default 50-connection pools × many app instances. **Why it fails:** exceeds the DB's max_connections → rejections; more connections also reduce throughput. **Better:** size pools from Little's Law (small); add a connection pooler. (See [database-capacity-and-connection-pools](../11-estimation-and-bottleneck-analysis/database-capacity-and-connection-pools.md).)

## 10. Ignoring p99 (optimizing/SLO-ing on average)
**Bad:** "Average latency is 20ms, we're fine." **Why it fails:** fan-out amplifies the tail; many users hit the slow p99. **Better:** SLO and optimize on p99; reduce the tail. (See [latency-budgets-and-tail-latency](../11-estimation-and-bottleneck-analysis/latency-budgets-and-tail-latency.md).)

## 11. Designing for millions of users without estimating
**Bad:** Shard + multi-region "because millions of users." **Why it fails:** MAU ≠ RPS ≠ data size; unjustified, undefendable complexity. **Better:** estimate the pressured dimension; keep it simple until a number forces complexity. (See [architecture-fundamentals](../00-foundations/architecture-fundamentals.md).)

## 12. One table / one access pattern for everything
**Bad:** Force all queries onto one table/store. **Why it fails:** no store serves all access patterns well → scatter-gather, weak transactions, scaling walls. **Better:** polyglot persistence; match the store to each access pattern. (See [sql-vs-document-vs-key-value-vs-wide-column](../03-databases-and-data-modeling/sql-vs-document-vs-key-value-vs-wide-column.md).)

## 13. Making everything synchronous
**Bad:** Inline every slow/external call. **Why it fails:** latency coupled to the slowest dependency; cascading overload. **Better:** async (queue + workers) for slow/heavy/unreliable work. (See [synchronous-vs-asynchronous-processing](../02-compute-and-architecture/synchronous-vs-asynchronous-processing.md).)

## 14. Making everything asynchronous
**Bad:** Queue even fast, needed-now reads. **Why it fails:** needless latency, polling, duplicates, complexity. **Better:** async only where it pays off. (Same ref.)

## 15. Multi-region without an availability/latency requirement
**Bad:** Go multi-region "for scale." **Why it fails:** cross-region consistency + huge cost/ops with no requirement. **Better:** single-region multi-AZ unless global latency/full-region survival/residency is required. (See [multi-region-architecture](../06-distributed-systems/multi-region-architecture.md).)

## 16. Ignoring authorization and tenant isolation
**Bad:** Check authn but not resource-level authz / tenant scoping. **Why it fails:** IDOR / cross-tenant leaks (the #1 vulnerability class). **Better:** resource-level authz on every endpoint + tenant_id scoping + RLS backstop. (See [authentication-authorization-and-rbac](../08-security/authentication-authorization-and-rbac.md), [multi-tenant-isolation](../08-security/multi-tenant-isolation.md).)

## 17. Ignoring observability
**Bad:** No metrics/traces/logs; "we'll add them later." **Why it fails:** can't diagnose incidents → guess / add servers randomly; high MTTR. **Better:** the three pillars + symptom-based alerting from day one. (See [logs-metrics-and-traces](../09-observability-and-operations/logs-metrics-and-traces.md).)

## 18. Assuming backups work without restore testing
**Bad:** "We have backups." **Why it fails:** untested backups often don't restore / are too slow / stopped running — discovered during the disaster. **Better:** regular restore tests + measure restore time vs RTO; store backups separately. (See [backups-and-point-in-time-recovery](../07-reliability-and-resilience/backups-and-point-in-time-recovery.md).)

## 19. Over-engineering a system a simple architecture handles
**Bad:** Kafka + microservices + sharding + multi-region for a modest-RPS system. **Why it fails:** complexity cost (ops, failure surface, latency) with no benefit; can't defend it. **Better:** simplest adequate design; justify every component with a number. (The meta-mistake.)

## 20. Happy-path-only thinking
**Bad:** Design only the success flow. **Why it fails:** ignores timeouts, retries, duplicates, partial failures, concurrency — where real systems (and interviews) break. **Better:** always design failure paths + concurrency control. (The other meta-mistake.)

---

## The two meta-mistakes

Almost everything above is **over-engineering** (complexity without a justifying requirement) or **happy-path thinking** (ignoring failure/concurrency). Before adding anything, ask "what breaks if I remove this?"; before finishing, ask "what happens when this fails / under concurrency?"

---

## Exercise

Review a design you did and audit it against this list. Did you add anything unjustified by a number (over-engineering)? Did you handle timeouts/retries/duplicates/concurrency (happy-path)? Fix the two biggest offenders.

**Next:** [mock-interviews](./mock-interviews.md)
