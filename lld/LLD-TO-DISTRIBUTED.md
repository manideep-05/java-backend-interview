# From In-Process LLD to Distributed Systems

The `production/` solutions solve concurrency **inside one JVM**. SDE2 interviews often end with "now scale it across many servers." The reassuring truth: the *same correctness ideas* carry over — only the mechanism changes. This page maps each in-process fix to its distributed equivalent, so you can answer the scale-up follow-up confidently.

> This maps directly to real backend work: idempotency, retries, dead-letter queues, the outbox pattern, atomic DB updates. Speak from that experience.

---

## The core mapping

| Concern | In-process (this repo's `production/`) | Distributed equivalent |
|---------|----------------------------------------|------------------------|
| "Grab the last unit" (seat/spot/stock) | `AtomicReference.compareAndSet` / CAS loop | **Conditional DB update**: `UPDATE ... SET status='TAKEN' WHERE id=? AND status='FREE'` and check rows-affected; or optimistic locking with a `version` column |
| Mutual exclusion on a resource | `synchronized` / `ReentrantLock` | **Distributed lock**: Redis `SET key val NX PX ttl` (Redlock), ZooKeeper/etcd lease. Always with a TTL to survive crashes |
| No-oversell counter | `AtomicInteger` CAS decrement | Atomic DB decrement `UPDATE ... SET qty=qty-? WHERE qty>=?`; or Redis `DECR` with a guard |
| Don't process twice (payment, coupon) | idempotency key in a `ConcurrentHashMap` / `set.add()` | **Idempotency key persisted** in a DB/Redis with a unique constraint; dedupe on retry |
| Async work off the hot path | `ThreadPoolExecutor` + `BlockingQueue` | **Message queue**: RabbitMQ / SQS / Kafka; workers consume |
| At-least-once + no lost work | retry + in-memory DLQ | **Retries with backoff + a real Dead-Letter Queue**; consumers made idempotent |
| Reliable "update DB then publish event" | (not needed in one process) | **Outbox pattern**: write the event to an outbox table in the same transaction, a relay publishes it — avoids the dual-write problem |
| Hold/expiry (seat, cache TTL) | timestamp + sweeper thread | TTL on a Redis key; or a scheduled reaper job |
| Per-key striped locks | lock per room/account | Partition/shard by key; the DB row or a per-key distributed lock is the unit |

---

## Three patterns worth saying by name

**1. Idempotency**
"Exactly-once" delivery doesn't really exist over a network — you get **at-least-once**, so make consumers idempotent: dedupe by a stable id (idempotency key, message id) using a unique constraint. (This is the distributed version of the `set.add()` / idempotency-map trick in the checkout, discount, and pub/sub problems.)

**2. Outbox pattern (dual-write problem)**
If you "save the order to the DB" and then "publish an order-placed event" as two separate operations, a crash in between loses the event (or double-sends). The outbox pattern writes the event into an `outbox` table **in the same DB transaction** as the business change; a separate relay reads the outbox and publishes, marking rows sent. Now the event and the state change are atomic. Pair with retries → DLQ for publish failures.

**3. Optimistic vs pessimistic concurrency**
- **Pessimistic**: lock the row/resource up front (`SELECT ... FOR UPDATE`, distributed lock). Simple, but reduces throughput and risks deadlocks.
- **Optimistic**: don't lock; read a `version`, do the work, then `UPDATE ... WHERE version=?`. If 0 rows change, someone else won — retry. This is the DB form of CAS, and it's how most high-throughput systems avoid overselling.

---

## Where LLD stops and HLD begins
- **LLD** (this repo): classes, interfaces, responsibilities, in-process correctness and concurrency. "Design the parking-lot *objects*."
- **HLD / system design**: services, databases, caches, queues, partitioning, replication, load balancing. "Design parking as a *service* for 1000 lots."
- The bridge: your clean LLD interfaces become service boundaries; your in-process atomic claim becomes a DB conditional update or a distributed lock; your in-memory queue becomes Kafka/SQS.

If an interviewer pushes past LLD, name this boundary explicitly ("that's a system-design concern; at the LLD level I'd expose it behind this interface, and the distributed implementation would use X") — it shows range without losing focus.
