# 19 — Messaging & Asynchronous Processing

> **Priority: P1.** Backend-relevant async concepts for building *reliable* processing — not an HLD course. The signal is delivery guarantees, idempotent consumers, and the outbox pattern, plus knowing when a queue is overkill.

**Prerequisites:** [06 Concurrency](../06-java-concurrency/), [13 Transactions](../13-spring-transactions/), [17 DB transactions](../17-database-transactions-and-isolation/).
**Related:** [18 API](../18-api-engineering/), [22 Debugging](../22-debugging-and-production-engineering/).

---

## A. Concept & intuition
Async processing decouples a request from slow/at-risk work (notifications, exports, downstream calls), smoothing load and improving latency and resilience. A message queue adds durability and buffering between producer and consumer (queue-based load leveling). The cost is complexity: delivery is not exactly-once in practice, so you must design for duplicates, retries, ordering, and failure.

## B. Core concepts

### Sync vs async; threads vs queues
- In-process async (an executor / `@Async`) is simple but **not durable** — if the JVM dies, in-flight work is lost, and it doesn't level load across instances.
- A **message broker** (RabbitMQ, Kafka) is durable, buffers spikes, and decouples producer/consumer lifecycles — at the cost of operating a broker and handling its semantics.

### Delivery guarantees (and their limits)
- **At-most-once:** may lose messages (fire-and-forget). Rarely acceptable.
- **At-least-once:** never loses, **may duplicate** (the common default). Requires **idempotent consumers**.
- **"Effectively/exactly-once":** achieved end-to-end only with idempotency/dedup or transactional semantics; true exactly-once delivery across a network is not generally possible — you make *processing* effectively-once via dedup.

### Idempotent consumers
Because at-least-once can redeliver, the consumer must produce the same result on reprocessing: dedup by a message id (a processed-ids table / unique constraint), or make the operation naturally idempotent (upsert). This is the single most important reliability practice ([17 DBTX-09](../17-database-transactions-and-isolation/)).

### Retries, backoff, DLQ, poison messages
Transient failures → retry with backoff; a **poison message** (always fails) must be routed to a **dead-letter queue** after N attempts so it doesn't block the queue/partition. Monitor the DLQ.

### Ordering & partitioning
Global ordering is expensive; Kafka gives ordering **within a partition**, keyed by a partition key (e.g., per-document id), so related messages stay ordered while unrelated ones parallelize. Choose keys to balance ordering needs vs parallelism.

### Backpressure & consumer concurrency
Bounded prefetch/consumer concurrency prevents a fast producer from overwhelming consumers or memory ([06](../06-java-concurrency/)). Scale consumers within ordering constraints.

### The dual-write problem & the transactional outbox
Writing to the DB **and** publishing a message are two systems — a crash between them loses the message or publishes without the DB change (**dual-write** inconsistency). The **outbox pattern** fixes it: in **one local transaction**, write the business change *and* an `outbox` row; a separate relay/CDC reads the outbox and publishes (at-least-once), marking rows sent. This gives atomicity locally + eventual consistency, avoiding distributed transactions ([13 §9](../13-spring-transactions/)).

### Scheduled jobs on multiple instances
A `@Scheduled` job runs on **every** instance by default — duplicate execution. Use a distributed lock (ShedLock, DB advisory lock) or a leader election so only one instance runs it ([XT-10](../27-cross-topic-interview-scenarios/)).

## C. Code — idempotent consumer + outbox
```java
// Idempotent consumer: dedup by message id with a unique constraint
@Transactional
public void onMessage(OrderEvent e) {
    if (!processed.tryInsert(e.messageId())) { // unique(message_id); false => already handled
        return;                                 // duplicate delivery -> no-op
    }
    applyBusinessChange(e); // in the SAME tx as the dedup insert -> atomic
}
```
```java
// Outbox: business write + outbox row in ONE transaction
@Transactional
public void placeOrder(Order o) {
    orderRepo.save(o);
    outboxRepo.save(new OutboxEvent("OrderPlaced", toJson(o))); // same tx -> no dual-write gap
}
// A separate relay polls outbox (or CDC) and publishes, then marks sent (at-least-once).
```

## D. Trade-offs

| Choice | Pros | Cons | When |
|---|---|---|---|
| In-process executor | simple, low latency | not durable, single-instance | best-effort, fast side tasks |
| RabbitMQ | routing, per-message ack, DLQ | ordering/throughput limits | task queues, work distribution |
| Kafka | high throughput, partition ordering, replay | heavier, ordering only per partition | event streams, high volume |
| At-least-once + idempotent | no loss, correct | must dedup | default reliable processing |
| Outbox | no dual-write gap | extra table + relay, eventual | DB change + publish atomically |

## E. Interview Q&A

### MSG-01 (P1) — When do you use a queue vs an in-process executor?
**Strong answer:** An in-process executor (`@Async`) is fine for best-effort, fast, non-critical side work, but it's lost on crash and can't level load across instances. I use a durable broker when the work must survive restarts, needs buffering against spikes, or must be distributed/retried reliably. For a simple "send an email after commit," an executor + outbox may suffice; for high-volume event processing, Kafka. I don't add a broker's operational cost unless durability/decoupling justify it.

### MSG-02 (P1) — Explain the delivery guarantees and their practical limits.
**Strong answer:** At-most-once can lose messages; at-least-once never loses but can duplicate; exactly-once delivery across a network isn't generally achievable, so systems default to at-least-once and achieve effectively-once *processing* via idempotency/dedup or transactional semantics. So in practice I assume duplicates and design idempotent consumers.

<a id="msg-03"></a>
### MSG-03 (P1) — How do you build an idempotent consumer, and why is it necessary?
**Strong answer:** Because at-least-once delivery redelivers on failures/timeouts, the consumer must make reprocessing a no-op: I dedup on a stable message id using a processed-ids table with a unique constraint (insert in the same transaction as the business change so they're atomic), or I make the operation naturally idempotent with an upsert. Without this, a redelivery double-applies — the duplicate-notification/double-charge bug. The unique constraint also handles two concurrent deliveries racing.

### MSG-04 (P1) — What is a poison message and how do you handle it?
**Strong answer:** A message that always fails processing (bad data, a bug) — without handling it blocks the queue/partition as it's retried forever. After a bounded number of attempts I route it to a dead-letter queue for inspection/replay, alert on DLQ growth, and keep processing the rest. I also distinguish transient failures (retry with backoff) from permanent ones (DLQ immediately).

### MSG-05 (P1) — What is the dual-write problem and how does the outbox pattern solve it?
**Strong answer:** Writing to the DB and publishing a message are two systems; a crash between them leaves them inconsistent (message lost or published without the DB change). The outbox pattern writes the business row and an outbox row in a single local transaction, then a relay (polling or CDC) publishes outbox rows at-least-once and marks them sent. It trades immediate consistency for eventual consistency but avoids fragile distributed transactions, and consumers dedup since delivery is at-least-once.

### MSG-06 (P2) — How does Kafka handle ordering?
**Strong answer:** Ordering is guaranteed only within a partition. Producers choose a partition key (e.g., document id) so all messages for one key land in one partition and stay ordered, while different keys spread across partitions for parallelism. Global ordering would require a single partition (no parallelism), so I key by the entity whose order matters.

## F. Production relevance
Reliable async processing is where the duplicate-notification, lost-event, and stuck-queue incidents live. The patterns that prevent them: assume at-least-once and make consumers idempotent (dedup + unique constraint), send side effects via `afterCommit`/outbox to avoid dual-write gaps, route poison messages to a DLQ, bound consumer concurrency for backpressure, key by the entity that needs ordering, and guard scheduled jobs with a distributed lock so they don't run on every instance. These map directly to [DBG-06/12](../22-debugging-and-production-engineering/) and [XT-03/10](../27-cross-topic-interview-scenarios/).

## G. Revision

### 10 key takeaways
1. Async decouples slow/risky work; queues add durability + load leveling.
2. In-process async isn't durable; brokers are.
3. At-least-once is the default; duplicates happen.
4. Exactly-once *processing* = idempotency/dedup, not magic delivery.
5. Idempotent consumers: dedup by message id + unique constraint, same tx.
6. Poison messages → DLQ after N attempts; monitor it.
7. Kafka orders within a partition; key by the ordering entity.
8. Bounded concurrency/prefetch = backpressure.
9. Outbox solves dual-write (business + outbox in one tx, relay publishes).
10. `@Scheduled` runs on every instance → distributed lock/leader.

### 10 rapid-fire
1. Default guarantee? (at-least-once) 2. Consumer must be? (idempotent) 3. Always-failing msg? (poison→DLQ) 4. Kafka ordering scope? (partition) 5. Dual-write fix? (outbox) 6. Dedup backed by? (unique constraint) 7. Scheduled job on N instances? (runs N times) 8. Exactly-once delivery possible? (no, dedup) 9. Backpressure via? (bounded prefetch) 10. Durable vs executor? (broker durable)

### 5 tricky
1. Why did a retried message send two emails? (non-idempotent) 2. Why not distributed transaction for DB+publish? (fragile→outbox) 3. Why did one bad message stall the queue? (poison, no DLQ) 4. Why did a cron run twice? (every instance) 5. Why can't you get true exactly-once delivery?

### 5 exercises
1. Build an idempotent consumer with a processed-ids unique constraint; replay a message → one effect.
2. Implement the outbox pattern (business + outbox in one tx) with a polling relay.
3. Configure a DLQ and route a poison message after N retries.
4. Key Kafka messages by document id and show per-key ordering.
5. Guard a `@Scheduled` job with ShedLock so only one instance runs it.

### One-page revision sheet
> Async decouples; queues=durable + load leveling (executor=not durable). **At-least-once default → duplicates → idempotent consumers** (dedup by message id + unique constraint, same tx). Exactly-once = effectively-once *processing* via dedup, not delivery. Poison msg → DLQ after N; monitor. Kafka orders **within partition**; key by ordering entity. Bounded prefetch = backpressure. **Outbox** = business row + outbox row in one local tx, relay publishes (fixes dual-write). @Scheduled runs on every instance → distributed lock (ShedLock)/leader.

### Prerequisites to revisit
- afterCommit & tx boundaries (ch.13); idempotency/unique constraints (ch.17); concurrency backpressure (ch.06).
