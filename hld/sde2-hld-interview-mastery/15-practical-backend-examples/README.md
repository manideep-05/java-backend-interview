# Practical Backend Examples (Java / Spring Boot)

> Phase 15 of the curriculum. These show how the HLD decisions in this repo translate into code — concise, focused snippets, not full applications.

## Why this section exists

In an HLD interview you *name* a pattern ("I'd use an idempotency key," "transactional outbox," "optimistic locking"). These examples let you back that up if asked "how would you implement it?" and are especially useful in a **mixed HLD/LLD round**. For a Java/Spring + RabbitMQ engineer, seeing the code reinforces the architectural reasoning.

## Important caveats (read first)

- **These snippets illustrate the HLD concept — they are NOT production-complete.** Real code needs error handling, metrics, security hardening, config, and tests beyond what's shown. A code snippet is never sufficient for production security or reliability.
- **The architectural concern lives above the code.** Each example notes *which concern belongs in application code vs infrastructure vs the database vs the messaging system* — because a common SDE-2 point is knowing where a responsibility belongs.
- **Prefer framework/library features over hand-rolled code** where they exist (Spring `@Transactional`, Spring Retry, Resilience4j, `@Version` for optimistic locking). The examples favor these.
- Versions/APIs evolve; treat these as patterns, not copy-paste.

## The 15 examples

| # | Example | HLD concept | Where the concern lives |
|---|---|---|---|
| 1 | [idempotency-key-example](./idempotency-key-example.md) | Safe retries, no double-effect | App (key store) + DB (unique constraint) |
| 2 | [database-transaction-example](./database-transaction-example.md) | Atomicity, ACID | DB + app (`@Transactional`) |
| 3 | [optimistic-locking-example](./optimistic-locking-example.md) | Concurrent-write safety | DB (`@Version`) + app (retry on conflict) |
| 4 | [redis-caching-example](./redis-caching-example.md) | Cache-aside + invalidation | App + Redis |
| 5 | [rate-limiting-example](./rate-limiting-example.md) | Distributed rate limiting | App + Redis (atomic Lua) |
| 6 | [message-producer-consumer-example](./message-producer-consumer-example.md) | Async messaging | App + broker (RabbitMQ) |
| 7 | [retry-and-backoff-example](./retry-and-backoff-example.md) | Resilient retries | App (Spring Retry) |
| 8 | [dead-letter-handling-example](./dead-letter-handling-example.md) | Poison-message isolation | Broker (DLX) + app |
| 9 | [transactional-outbox-example](./transactional-outbox-example.md) | No dual-write | DB (outbox table) + app (relay) |
| 10 | [pagination-example](./pagination-example.md) | Scalable listing | App + DB (cursor + index) |
| 11 | [connection-pool-example](./connection-pool-example.md) | Bounded DB connections | App (HikariCP config) |
| 12 | [timeout-configuration-example](./timeout-configuration-example.md) | Bound every call | App (client configs) |
| 13 | [circuit-breaker-example](./circuit-breaker-example.md) | Stop cascading failure | App (Resilience4j) |
| 14 | [graceful-shutdown-example](./graceful-shutdown-example.md) | Zero-downtime stop | App + platform (SIGTERM) |
| 15 | [metrics-and-tracing-example](./metrics-and-tracing-example.md) | Observability | App (Micrometer/OTel) + infra |

## How to use

Read the example alongside its concept chapter (linked in each file). In an interview, you don't write all this code — you *reference* the pattern and, if pushed, sketch the key part (e.g., the atomic idempotency-key insert, the `@Version` field, the outbox transaction). The goal is to connect "I'd use X" to "here's the mechanism."

**Especially relevant to your resume (Manideep):** examples 1, 6, 7, 8, 9 (your RabbitMQ notification platform), 2, 3, 10, 11 (your Oracle optimization + workflows), 1, 13, 12 (your EPDDMS↔VDMS idempotent sync). See [YOUR-RESUME-STUDY-GUIDE](../YOUR-RESUME-STUDY-GUIDE.md).

---
*Return to [README](../README.md).*
