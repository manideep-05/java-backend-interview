# Roadmap — Study Order and Tracks

This roadmap turns the repository into a sequenced course. It assumes you work full-time and can study a few focused hours per week. Progress is measured by what you can **explain and defend**, not by files read.

Two tracks:

- **Track A — Core interview readiness.** The concepts and designs with the highest payoff for SDE-2 interviews. Finish this and you can pass most SDE-2 HLD rounds.
- **Track B — Advanced depth.** Specialized distributed systems, multi-region, advanced DB scaling. Do this after Track A is solid, or to target higher bands / stronger companies.

> Rule: do not advance to the next stage until you meet its **exit criteria**. Reading ahead is fine; skipping practice is not.

---

## How expectations differ by level

| Dimension | SDE-1 | **SDE-2 (your target)** | Staff / Principal |
|---|---|---|---|
| Requirements | Takes them as given | **Clarifies functional + non-functional, sets scope** | Challenges the problem framing itself |
| Estimation | Rough | **Order-of-magnitude with shown math, ties to design** | Cost/capacity modeling across org |
| Architecture | One known pattern | **Chooses simplest adequate design, justifies vs alternatives** | Designs for org-wide reuse, migration paths |
| Failures | Happy path | **Timeouts, retries, idempotency, partial failure** | Multi-region failure, correlated failure, blast radius |
| Scope | Single service | **Service + its dependencies** | Platform, multiple teams, long-term evolution |

You are **not** expected to design a global multi-region system to pass SDE-2. You **are** expected to know when it becomes necessary and why, and to default to the simplest design that meets stated requirements.

---

## Track A — Core interview readiness

### Stage A1 — Foundations & estimation
**Objective:** Speak the language (latency, throughput, availability) and estimate scale fast.
**Prerequisites:** Basic backend experience.
**Read:**
- `00-foundations/what-is-system-design.md`
- `00-foundations/requirements-and-constraints.md`
- `00-foundations/latency-throughput-and-capacity.md`
- `00-foundations/back-of-the-envelope-estimation.md`
- `00-foundations/interview-communication-framework.md`

**Explain aloud:** functional vs non-functional requirements; latency vs throughput; p50 vs p99; MAU vs DAU vs RPS vs peak concurrency.
**Exercises:** First 8 estimation drills in `11-estimation-and-bottleneck-analysis/estimation-cheat-sheet.md`.
**Exit criteria:** Given "1M DAU, 20 req/user/day, 5× peak", you compute average and peak RPS in under 2 minutes and state what it implies for app and DB tiers.

### Stage A2 — Networking, HTTP, APIs, load balancing
**Objective:** Design clean APIs and know what sits in front of your service.
**Read:** `01-networking-and-api-design/` — `dns-tcp-tls-http.md`, `rest-and-api-design.md`, `pagination-filtering-and-sorting.md`, `authentication-and-authorization.md`, `api-gateway-and-load-balancing.md`, `rate-limiting.md`.
**Explain aloud:** what happens when you type a URL and press enter; idempotent vs safe HTTP methods; L4 vs L7 load balancing.
**Exercise:** Design the REST API for the DMS (documents, revisions, upload).
**Exit criteria:** You can design a paginated, authenticated, idempotent API and explain where a load balancer and gateway fit.

### Stage A3 — Application architecture & the scaling journey
**Objective:** Scale a system from 1 user to millions, knowing each bottleneck.
**Read:** `02-compute-and-architecture/` (all), especially `monolith-vs-modular-monolith-vs-microservices.md`, `horizontal-vs-vertical-scaling.md`, `stateless-vs-stateful-services.md`, then the **scaling journey** in `00-foundations/architecture-fundamentals.md`.
**Explain aloud:** why adding app servers can make the DB problem worse; stateless vs sticky sessions.
**Exercise:** Draw the architecture at each of the 6 scaling stages for the DMS.
**Exit criteria:** For any added component (replica, cache, queue) you can name the bottleneck it solves **and** the new one it introduces.

### Stage A4 — Databases & data modeling
**Objective:** Treat DB choice as an architectural decision.
**Read:** `03-databases-and-data-modeling/` — `relational-vs-nosql.md`, `indexing-and-query-optimization.md`, `transactions-and-isolation.md`, `replication-and-read-replicas.md`, `partitioning-and-sharding.md`, `consistency-models.md`.
**Explain aloud:** ACID isolation levels; what replication lag causes; how to pick a shard key; what a hot partition is.
**Exercise:** Pick and justify a database for payments, news feed, chat, and the DMS.
**Exit criteria:** You can defend "SQL first, replicas when reads dominate, shard only after measuring" with concrete triggers.

### Stage A5 — Caching & Redis
**Read:** `04-caching/` — `caching-fundamentals.md`, `cache-aside-read-through-write-through.md`, `cache-invalidation-and-consistency.md`, `stampedes-hot-keys-and-penetration.md`, `local-vs-distributed-cache.md`, `redis-design-and-failure-modes.md`.
**Explain aloud:** cache-aside read + write path; stampede and 3 fixes; what happens when Redis is down.
**Exercise:** Add caching to the DMS document-metadata read path; define invalidation.
**Exit criteria:** You never say "add Redis" without stating the invalidation strategy and the Redis-down fallback.

### Stage A6 — Messaging & async processing
**Read:** `05-messaging-and-event-driven-systems/` — `queues-vs-pub-sub-vs-event-streams.md`, `message-delivery-semantics.md`, `retries-backoff-and-jitter.md`, `dead-letter-queues.md`, `idempotency-and-deduplication.md`, `transactional-outbox-and-cdc.md`, `backpressure-and-consumer-lag.md`.
**Explain aloud:** at-least-once vs effectively-once; why a consumer crash after a side effect causes duplicates; the outbox pattern.
**Exercise:** Design the notification service async flow with idempotent consumers and a DLQ.
**Exit criteria:** You can explain why "exactly-once delivery" is not the same as "no duplicate side effects."

### Stage A7 — Distributed systems basics
**Read:** `06-distributed-systems/` — `partial-failures-and-timeouts.md`, `retries-and-duplicate-execution.md`, `consistency-and-cap-theorem.md`, `distributed-locks.md`, `id-generation-and-sequences.md`, `exactly-once-processing.md`, `circuit-breakers-and-bulkheads.md`.
**Explain aloud:** CAP and its limits; the unsafe check-then-charge payment sequence and a safer version; fencing tokens.
**Exit criteria:** You can turn a naive "check then act" flow into an idempotent, concurrency-safe one and name the remaining limitations.

### Stage A8 — Core design problems
**Read & work:** `12-system-design-problems/` — url-shortener, rate-limiter, notification-service, file-storage-service, document-management-system, chat-messaging-system, news-feed, payment-system.
**Exercise:** Do each under a 45-minute timer using `00-foundations/interview-communication-framework.md`.
**Exit criteria:** You complete 4 designs end-to-end within time, including failure handling and 10 follow-ups each.

### Stage A9 — Mocks & revision
**Read:** `13-interview-practice/` (all) and `14-revision/` (all).
**Exercise:** 5 mock interviews from `13-interview-practice/mock-interviews.md`, scored with the rubric.
**Exit criteria:** Average ≥ 3.5/5 across all rubric dimensions in `INTERVIEW-CHECKLIST.md`.

---

## Track B — Advanced depth

Do after A8. Order:

1. `03-databases-and-data-modeling/distributed-transactions.md`, `time-series-and-search-databases.md`, `schema-migrations-and-data-evolution.md`.
2. `06-distributed-systems/` — `quorum-reads-and-writes.md`, `leader-election-and-split-brain.md`, `clock-skew-and-logical-clocks.md`, `multi-region-architecture.md`.
3. `05-messaging-and-event-driven-systems/` — `saga-pattern.md`, `event-sourcing-and-cqrs.md`, `kafka-internals-and-partitioning.md`.
4. `07-reliability-and-resilience/` (all) and `09-observability-and-operations/` (all).
5. Advanced designs: `video-streaming-platform.md`, `ride-hailing-system.md`, `ticket-booking-system.md`, `metrics-monitoring-platform.md`, `cloud-file-sync.md`.
6. `10-storage-and-content-delivery/` (all).

**Track B exit criteria:** You can design a multi-region system *and* argue when you would **not**, reason about replication lag and consistency trade-offs, and diagnose a bottleneck from metrics rather than guessing.

---

## Weekly plan templates

**~6 hours/week, 12 weeks (Track A):** 1 stage every ~1.5 weeks; A8 gets 2 weeks.

**~10 hours/week, 8 weeks:** A1–A2 (wk1), A3 (wk2), A4 (wk3), A5–A6 (wk4), A7 (wk5), A8 (wk6–7), A9 (wk8).

Track progress in `PROGRESS.md`. Reassess readiness with `INTERVIEW-CHECKLIST.md` after Stage A8 and again after A9.
