# Glossary

Terms used across the repository. Definitions are interview-oriented: what it means and why it matters, not formal CS definitions. Where a term has a dedicated chapter, follow the link.

> Convention: anything described as a "heuristic" is an order-of-magnitude rule of thumb that depends on workload and hardware — never a universal law.

---

## A

**ACID** — Atomicity, Consistency, Isolation, Durability: the guarantees of a classic transaction. See [transactions](./03-databases-and-data-modeling/transactions-and-isolation.md).

**Active-active** — Multiple regions/replicas all serving writes. Higher availability, harder consistency.

**Active-passive** — One region serves; another stands by for failover. Simpler, but wasted standby capacity and failover delay.

**At-least-once** — A message may be delivered more than once; consumers must be idempotent. The common default in queues.

**At-most-once** — A message is delivered zero or one times; may be lost. Rare in business systems.

**Availability** — Fraction of time a system correctly serves requests, often expressed in "nines" (99.9% = ~8.76h downtime/year).

**Avalanche (cache)** — Many keys expire at once, flooding the backend. Fixed with TTL jitter.

## B

**Backpressure** — A mechanism for a slow consumer to signal producers to slow down, preventing unbounded queue growth. See [backpressure](./05-messaging-and-event-driven-systems/backpressure-and-consumer-lag.md).

**Back-of-the-envelope** — Order-of-magnitude estimation done quickly to size a system. See [estimation](./00-foundations/back-of-the-envelope-estimation.md).

**Bloom filter** — Space-efficient probabilistic set; "definitely not present" or "maybe present." Used to prevent cache penetration and skip disk reads.

**Blue-green deployment** — Two identical environments; switch traffic from old (blue) to new (green) instantly, roll back by switching back.

**Bulkhead** — Isolating resources (thread pools, connection pools) so one failing dependency can't exhaust everything. See [circuit breakers & bulkheads](./06-distributed-systems/circuit-breakers-and-bulkheads.md).

## C

**Cache-aside (lazy loading)** — App checks cache; on miss, reads DB and populates cache. Most common pattern. See [caching](./04-caching/cache-aside-read-through-write-through.md).

**Canary deployment** — Release to a small % of traffic first, watch metrics, then ramp.

**CAP theorem** — Under a network partition, you choose consistency or availability. See [CAP](./06-distributed-systems/consistency-and-cap-theorem.md).

**CDC (Change Data Capture)** — Streaming a database's change log (e.g., WAL) to other systems. Basis of reliable outbox/replication.

**CDN** — Geographically distributed caches serving static/edge content close to users. See [CDN](./10-storage-and-content-delivery/cdn-and-edge-caching.md).

**Circuit breaker** — Stops calling a failing dependency after a threshold, fails fast, periodically probes recovery. See [circuit breaker](./07-reliability-and-resilience/circuit-breaker-pattern.md).

**Consistency (distributed)** — Agreement about the latest value across replicas. Ranges from strong to eventual. See [consistency models](./03-databases-and-data-modeling/consistency-models.md).

**Consistent hashing** — Hashing scheme that minimizes key remapping when nodes are added/removed. Used by distributed caches and sharded stores.

**CQRS** — Command Query Responsibility Segregation: separate write model from read model(s). See [event sourcing & CQRS](./05-messaging-and-event-driven-systems/event-sourcing-and-cqrs.md).

## D

**DAU / MAU** — Daily / Monthly Active Users. Different from RPS and concurrency.

**Dead-letter queue (DLQ)** — Where messages go after repeated processing failure, for inspection/replay. See [DLQ](./05-messaging-and-event-driven-systems/dead-letter-queues.md).

**Denormalization** — Duplicating data to avoid joins and speed reads, trading write complexity and consistency.

**Durability** — Once acknowledged, data survives crashes (e.g., written to disk/replicated).

## E

**Eventual consistency** — Replicas converge to the same value *eventually* if writes stop. Reads may be stale.

**Exactly-once (processing)** — A business effect happens once despite retries/duplicates. Achieved via idempotency + dedup, not by magic delivery. See [exactly-once](./06-distributed-systems/exactly-once-processing.md).

## F

**Fan-out** — One event producing many writes/deliveries (e.g., a post to all followers' feeds).

**Fencing token** — Monotonic number issued with a lock so a stale lock holder's writes are rejected. See [distributed locks](./06-distributed-systems/distributed-locks.md).

**Failover** — Switching to a standby when the primary fails.

## H

**Hot key / hot partition** — A single key or shard receiving disproportionate traffic, becoming a bottleneck. See [sharding](./03-databases-and-data-modeling/partitioning-and-sharding.md), [hot keys](./04-caching/stampedes-hot-keys-and-penetration.md).

**Heuristic** — Rule of thumb; order-of-magnitude, workload-dependent. Validate by measuring.

## I

**Idempotency** — Doing an operation multiple times has the same effect as once. Essential for safe retries. See [idempotency](./05-messaging-and-event-driven-systems/idempotency-and-deduplication.md).

**Isolation level** — How much concurrent transactions can interfere (Read Committed, Repeatable Read, Serializable, etc.).

**Index** — Auxiliary data structure (usually B-tree) that speeds lookups at the cost of write overhead and storage.

## L

**Latency** — Time for one operation to complete. Report percentiles (p50/p95/p99), not just averages.

**LSM tree** — Log-Structured Merge tree: write-optimized storage used by Cassandra, RocksDB, etc.

**Leader election** — Choosing one node to coordinate; must handle split-brain. See [leader election](./06-distributed-systems/leader-election-and-split-brain.md).

**Load balancer (L4 / L7)** — Distributes traffic. L4 = transport (TCP/UDP), L7 = application (HTTP-aware). See [LB](./01-networking-and-api-design/api-gateway-and-load-balancing.md).

## O

**Optimistic locking** — Detect conflicting concurrent writes via a version column; retry on conflict. No held locks.

**Outbox (transactional)** — Write the event to a DB table in the same transaction as the business change; a relay publishes it. Avoids dual-write inconsistency. See [outbox](./05-messaging-and-event-driven-systems/transactional-outbox-and-cdc.md).

## P

**Partition (network)** — Nodes can't communicate; the "P" in CAP.

**Partition (data) / sharding** — Splitting data across nodes by a key. See [sharding](./03-databases-and-data-modeling/partitioning-and-sharding.md).

**Percentile (p95/p99)** — The latency below which 95%/99% of requests fall. Tail latency dominates user experience at scale.

**Poison message** — A message that always fails processing; must be routed to a DLQ to avoid blocking.

## Q

**Quorum** — A majority (or configured count) of replicas that must acknowledge a read/write. R + W > N gives strong-ish consistency. See [quorum](./06-distributed-systems/quorum-reads-and-writes.md).

**Queue** — Point-to-point async channel; each message consumed by one consumer. See [queues](./05-messaging-and-event-driven-systems/queues-vs-pub-sub-vs-event-streams.md).

## R

**Rate limiting** — Capping request rate per client to protect the system. Algorithms: token bucket, leaky bucket, sliding window. See [rate limiting](./01-networking-and-api-design/rate-limiting.md).

**Read replica** — A copy of the DB that serves reads, offloading the primary; introduces replication lag. See [replication](./03-databases-and-data-modeling/replication-and-read-replicas.md).

**Replication lag** — Delay between a write on the primary and its visibility on a replica.

**RPO / RTO** — Recovery Point Objective (max acceptable data loss) / Recovery Time Objective (max acceptable downtime). See [DR](./07-reliability-and-resilience/disaster-recovery.md).

**RPS / QPS** — Requests / Queries per second.

## S

**Saga** — A sequence of local transactions with compensating actions for failure, replacing distributed transactions. See [saga](./05-messaging-and-event-driven-systems/saga-pattern.md).

**Serializable** — Strongest isolation; transactions behave as if run one at a time.

**Sharding** — See partition (data).

**SLI / SLO / SLA** — Service Level Indicator (a measured metric) / Objective (internal target) / Agreement (external contract with penalties). See [SLx](./07-reliability-and-resilience/slis-slos-and-slas.md).

**Split-brain** — Two nodes both believe they are leader; causes conflicting writes.

**Stampede (cache)** — Many requests miss the same expired hot key simultaneously and all hit the DB. See [stampede](./04-caching/stampedes-hot-keys-and-penetration.md).

**Stateless service** — Keeps no client session state locally; any instance can serve any request. Enables easy horizontal scaling. See [stateless](./02-compute-and-architecture/stateless-vs-stateful-services.md).

## T

**Throughput** — Operations completed per unit time (e.g., RPS). Distinct from latency.

**TTL** — Time To Live; expiry duration for a cache entry or record.

**Two-phase commit (2PC)** — Coordinator-based atomic commit across nodes; blocking and failure-prone, usually avoided in favor of sagas.

## W

**WAL (Write-Ahead Log)** — Durable append-only log written before applying changes; basis of crash recovery, replication, and CDC.

**Webhook** — An HTTP callback the server sends to a client-provided URL on an event. See [webhooks](./01-networking-and-api-design/webhooks-and-callbacks.md).

**Write amplification** — One logical write causing many physical writes (e.g., feed fan-out, LSM compaction).

**Write-through / write-behind / write-around** — Cache write strategies. See [caching patterns](./04-caching/cache-aside-read-through-write-through.md).
