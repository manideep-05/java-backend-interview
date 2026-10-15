# Pub/Sub Message Queue — LLD

Design an in-process publish/subscribe message broker (think a mini Kafka/RabbitMQ). High-value for backend roles: it exercises Observer, concurrency, retries, and dead-letter handling — the exact shape of production messaging systems.

---

## 1. Requirements

**Functional**
- Publishers publish messages to a **topic**.
- Subscribers **subscribe** to a topic and receive its messages.
- Fan-out: every subscriber of a topic gets every message (at-least-once).
- Retry a failing delivery; after N tries, send to a **dead-letter queue (DLQ)**.

**Non-functional**
- Publishing must not block on slow subscribers → **async** delivery.
- Many publishers/subscribers concurrently → thread-safe registries and queues.
- Backpressure when a consumer is slow.

---

## 2. Why these patterns

- **Observer / pub-sub**: a topic notifies its subscribers; publisher doesn't know who they are.
- **Producer/consumer**: each subscriber has a queue drained by a worker thread, decoupling publish latency from processing latency.
- **Strategy** (optional): the retry/backoff policy.

---

## 3. Critical flow

publish(topic, msg) → broker looks up topic's subscribers → enqueues the message to each subscriber's queue (non-blocking) → each subscriber's worker thread takes a message and invokes the handler → on failure, retry up to N; then route to the DLQ.

---

## 4. Edge cases & how to spot them

| Source | Edge | Handling |
|--------|------|----------|
| Slow consumer | One subscriber lags | Per-subscriber queue + worker; others unaffected. |
| Failure | Handler throws | Retry with backoff; after N, DLQ. |
| Delivery | Message lost on crash | At-least-once + idempotent consumers (dedupe by id). |
| Overload | Queue fills | Bounded queue + policy (block/drop). |
| Lifecycle | Unsubscribe mid-delivery | Thread-safe subscriber set; stop the worker cleanly. |
| No subscribers | Publish to empty topic | Message dropped or retained (define). |

---

## 5. Entities, relationships, responsibilities

- **Message**: id + payload (+ delivery attempt count).
- **Subscriber**: a handler + its own inbox queue + worker thread.
- **Topic**: name + set of subscribers.
- **Broker** (Facade): topic registry; publish/subscribe/unsubscribe; owns the DLQ.
- **MessageHandler** (interface): what a subscriber does with a message.

---

## 6. Class/interface design

```
Broker (Facade)
   ├── Map<topic, Set<Subscriber>>        OBSERVER registry
   ├── DLQ (failed messages)
   └── publish(topic, msg) -> enqueue to each subscriber (async)
Subscriber: BlockingQueue inbox + worker thread + MessageHandler + retry
```

### normal vs production
- `normal/` — synchronous fan-out: broker loops subscribers and calls the handler inline, with a simple retry-then-DLQ. Clear, single-threaded.
- `production/` — each subscriber owns a bounded `BlockingQueue` + a worker thread (producer/consumer); the broker's publish just enqueues (non-blocking); `ConcurrentHashMap`/`CopyOnWriteArraySet` for registries; retry with backoff then DLQ; clean shutdown drains workers.

---

## Interview Questions, Follow-ups & Traps

**Q: Why give each subscriber its own queue + worker instead of one shared loop?**
Isolation: a slow or failing subscriber only backs up its own queue, never blocking the publisher or other subscribers. This is the per-consumer-lag fix real brokers use.

**Q: At-least-once vs exactly-once delivery?**
Networks and retries force **at-least-once** — a message may be delivered more than once. "Exactly-once" is achieved by making consumers **idempotent** (dedupe by message id), not by the broker magically delivering once. (This is exactly the idempotency + outbox pattern in production messaging.)

**Q: How do retries and the DLQ work?**
On handler failure, retry up to N times (ideally with exponential backoff). If it still fails, move the message to a dead-letter queue for inspection/replay instead of blocking the pipeline or losing it. (Mirrors "5 retries then DLQ" setups.)

**Follow-ups**
- *"Ordering guarantees?"* — Per-subscriber single worker preserves order within a topic→subscriber; across subscribers there's no global order. Partitioning (like Kafka) scales ordered consumption.
- *"Backpressure?"* — Bounded queues; block or drop when full. Unbounded = OOM.
- *"Durability / crash safety?"* — Persist messages (outbox table / WAL) before ack; replay on restart.

**Traps**
- Synchronous fan-out blocking the publisher on a slow subscriber.
- Unbounded queues.
- Assuming exactly-once without idempotent consumers.
- Losing failed messages instead of dead-lettering them.
