# Architecture Defense Questions

> Related: [tradeoff-questions](./tradeoff-questions.md) · [common-interviewer-pushbacks](./common-interviewer-pushbacks.md) · [00-foundations/interview-communication-framework](../00-foundations/interview-communication-framework.md)

Interviewers challenge your decisions to see whether you reason or just recite. These are "why did you choose X?" / "why not Y?" questions with model answers. The pattern of a good defense: restate the requirement, give the reason, acknowledge the alternative and its trade-off, and name the trigger that would change your mind.

---

## The defense pattern

> "I chose X because [requirement-grounded reason]. The alternative Y would [trade-off]; I'd switch to it if [measurable condition]."

Never defend a choice as a universal fact ("X is best"). Always ground it in the requirement and name the condition under which you'd change.

---

## Architecture decisions (Q → model answer)

**Why a monolith / modular monolith, not microservices?**
→ With one team and a shared data model, microservices add network calls, distributed transactions, and ops overhead for no benefit. A modular monolith gives simplicity + clean boundaries, and I can extract a module into a service when a measured scaling or team-ownership pressure justifies it. (See [monolith-vs-microservices](../02-compute-and-architecture/monolith-vs-modular-monolith-vs-microservices.md).)

**Why not add Kafka / a queue here?**
→ Async messaging only pays off for slow/heavy/unreliable work or decoupling needs. If the operation is a fast, needed-now read, a queue adds latency, duplicates (idempotency), and ops for nothing. I'd add it when I have a concrete async requirement (e.g., CPU-heavy processing off the request path). (See [synchronous-vs-asynchronous-processing](../02-compute-and-architecture/synchronous-vs-asynchronous-processing.md).)

**Why PostgreSQL, not NoSQL?**
→ The data is relational with multi-entity transactions and evolving queries, and the volume fits one node — relational is simpler and gives ACID. NoSQL would push joins into the app and weaken transactions for scale I don't need. I'd move a specific write-heavy pattern to a wide-column store only when it outgrows a single primary. (See [relational-vs-nosql](../03-databases-and-data-modeling/relational-vs-nosql.md).)

**Why add a cache / why Redis?**
→ The read path is read-heavy on change-tolerant data, so a cache offloads the DB and cuts latency. I'd define the invalidation strategy and the Redis-down fallback (DB with a concurrency limiter). I would *not* cache strongly-consistent data or add Redis to mask a missing index. (See [caching-fundamentals](../04-caching/caching-fundamentals.md).)

**Why not shard the database?**
→ At the measured write rate and data size, a single tuned primary (+ replicas + cache) handles it for years; sharding adds cross-shard query/transaction complexity and ops. I'd shard only when writes/data approach the single-node limit, picking a shard key that keeps the common query single-shard. (See [partitioning-and-sharding](../03-databases-and-data-modeling/partitioning-and-sharding.md).)

**Why not multi-region?**
→ There's no stated latency/availability/residency requirement that justifies it. Single-region multi-AZ handles most failures far more cheaply, with strong consistency. Multi-region's cross-region consistency and cost are only worth it with a concrete requirement. (See [multi-region-architecture](../06-distributed-systems/multi-region-architecture.md).)

**Why eventual consistency here / why strong there?**
→ Per-path: strong where correctness demands it (money, inventory, uniqueness), eventual where staleness is harmless (feeds, counts, search). I pick the weakest model that's still correct, to preserve latency and availability. (See [consistency-models](../03-databases-and-data-modeling/consistency-models.md).)

**Why files in object storage, not the DB?**
→ The storage math (e.g., 180 TB of files vs 90 GB of metadata) makes blobs-in-DB a bloat/cost/backup problem; object storage is built for durable cheap blobs. Metadata + the object key go in the DB. (See [object-storage-and-file-uploads](../10-storage-and-content-delivery/object-storage-and-file-uploads.md).)

**Why a search engine / why not SQL LIKE?**
→ `LIKE '%term%'` full-scans at scale and lacks ranking/fuzzy. I'd start with DB full-text if modest, and move to a search engine (inverted index, fed via CDC) when scale or relevance needs demand it — accepting the sync pipeline + eventual consistency. (See [full-text-search-vs-database-search](../10-storage-and-content-delivery/full-text-search-vs-database-search.md).)

**Why WebSockets, not polling?**
→ Real-time push needs a persistent connection; polling is wasteful and higher-latency. I isolate the stateful connection layer and keep the rest stateless. (See [chat-messaging-system](../12-system-design-problems/chat-messaging-system.md).)

**Why a saga, not 2PC?**
→ 2PC blocks on coordinator failure, has a SPOF, and can't include an external provider; a saga keeps services available with local transactions + compensations, accepting eventual consistency. I'd co-locate transactionally-coupled data to avoid needing either. (See [saga-pattern](../05-messaging-and-event-driven-systems/saga-pattern.md).)

---

## How to handle "I disagree with that"

Don't cave or dig in. Restate the assumption your choice depends on, ask what changed, and adapt: "I chose eventual consistency assuming stale feed reads are fine. If we need read-your-write, I'd route recent reads to the primary — is that the requirement?" Revising gracefully is a *positive* signal (see [common-interviewer-pushbacks](./common-interviewer-pushbacks.md)).

---

## Exercise

For a design you've done, have someone (or yourself) challenge five decisions with "why X / why not Y?" Answer each with the defense pattern (requirement → reason → alternative + trade-off → switching condition). If you can't name the switching condition, you don't understand the decision yet.

**Next:** [tradeoff-questions](./tradeoff-questions.md)
