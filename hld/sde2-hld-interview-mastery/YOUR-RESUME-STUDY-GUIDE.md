# Your Resume → Repo Study Guide (Manideep)

This maps *your actual experience* to the repository, so you can (a) study the chapters that make your work "interview-legible," and (b) tell your stories in HLD language. Your background is unusually well-aligned with SDE-2 HLD: you've already built an outbox+DLQ notification platform, optimized 60M-row Oracle tables, and done idempotent cross-system sync. The job now is to **articulate what you built as system-design decisions and trade-offs**, and to generalize from "I did X at EIL" to "here's how I'd design X."

> How to use: for each experience below, read the linked chapters, rehearse the "tell it as HLD" script, and drill the likely follow-ups. Then do the mapped mock interview under a timer.

---

## 1. Asynchronous Notification Platform (Node.js, Express, RabbitMQ, outbox, 5 retries, DLQ, 15K/day, 13 apps)

**This is your strongest story — it's a near-complete SDE-2 messaging design you built in production.**

**Study (you already know most of this — study it to name it precisely):**
- [transactional-outbox-and-cdc](./05-messaging-and-event-driven-systems/transactional-outbox-and-cdc.md) — your outbox pattern; be able to explain the **dual-write problem** it solves.
- [retries-backoff-and-jitter](./05-messaging-and-event-driven-systems/retries-backoff-and-jitter.md) — your 5 retries; add the words **exponential backoff + jitter** and **retry budget**.
- [dead-letter-queues](./05-messaging-and-event-driven-systems/dead-letter-queues.md) — your DLQ; explain **poison messages** and **head-of-line blocking**.
- [message-delivery-semantics](./05-messaging-and-event-driven-systems/message-delivery-semantics.md) + [idempotency-and-deduplication](./05-messaging-and-event-driven-systems/idempotency-and-deduplication.md) — the **"at-least-once + idempotency = effectively-once"** framing (your reusable client's consumers need this).
- [rabbitmq-internals-and-routing](./05-messaging-and-event-driven-systems/rabbitmq-internals-and-routing.md) — your configurable department subscriptions = **topic/direct exchange routing**.
- [notification-service](./12-system-design-problems/notification-service.md) — the full design; **this is the problem you've lived.**
- Code: [15-practical-backend-examples/transactional-outbox-example](./15-practical-backend-examples/transactional-outbox-example.md), [message-producer-consumer-example](./15-practical-backend-examples/message-producer-consumer-example.md), [retry-and-backoff-example](./15-practical-backend-examples/retry-and-backoff-example.md), [dead-letter-handling-example](./15-practical-backend-examples/dead-letter-handling-example.md).

**Tell it as HLD (your 60-second story):**
> "I built a centralized notification platform. The core problem was the **dual-write** — updating the business DB and publishing to RabbitMQ aren't atomic, so I used the **transactional outbox**: write the event to an outbox table in the same transaction, and a relay publishes it, so no event is lost on a crash. Delivery is **at-least-once**, so consumers are **idempotent** (dedup on event id). Failed sends retry with backoff, up to 5 attempts, then route to a **DLQ** for inspection — avoiding poison messages blocking the pipeline. Department subscriptions are exchange routing. I packaged it as a reusable client, adopted by 13 apps."

**Likely follow-ups (and your answers):**
- *"A consumer crashed after sending but before acking — duplicate notification?"* → At-least-once redelivery, but idempotent consumers (dedup on notification id) make the re-send a no-op → effectively-once.
- *"Why the outbox instead of just publishing after the DB commit?"* → A crash between commit and publish loses the event; the outbox makes event creation atomic with the business change.
- *"15K/day spikes to a campaign — how do you not fall over?"* → Autoscale consumers on queue depth, prioritize transactional over bulk, shed/defer low-priority, limit concurrency to providers. ([backpressure-and-consumer-lag](./05-messaging-and-event-driven-systems/backpressure-and-consumer-lag.md).)
- *"RabbitMQ vs Kafka here?"* → RabbitMQ fits task distribution + routing + per-message ack and no replay need; Kafka if you needed retained/replayable events for many independent consumers.

---

## 2. ~90% API Optimization on 60M-row Oracle tables (4–5s → 300–500ms; indexes, execution plans, N+1, materialized views)

**This is a *diagnosis* story — exactly the "find the bottleneck, don't just add servers" skill interviewers prize.**

**Study:**
- [indexing-and-query-optimization](./03-databases-and-data-modeling/indexing-and-query-optimization.md) — your indexes + N+1 + execution plans. Name the **composite-prefix rule** and **EXPLAIN/execution plan reading**.
- [normalization-and-denormalization](./03-databases-and-data-modeling/normalization-and-denormalization.md) — your materialized views = **denormalized read models** (precompute to avoid expensive joins/aggregations).
- [scaling-bottleneck-diagnosis](./11-estimation-and-bottleneck-analysis/scaling-bottleneck-diagnosis.md) — the **index → cache → replica → shard ladder** (you did the cheapest, highest-leverage rung).
- [database-capacity-and-connection-pools](./11-estimation-and-bottleneck-analysis/database-capacity-and-connection-pools.md) — Little's Law + pool sizing.
- [database-internals-for-system-design](./03-databases-and-data-modeling/database-internals-for-system-design.md) — B-tree indexes (why your index worked).
- Code: [database-transaction-example](./15-practical-backend-examples/database-transaction-example.md), [pagination-example](./15-practical-backend-examples/pagination-example.md), [connection-pool-example](./15-practical-backend-examples/connection-pool-example.md).

**Tell it as HLD:**
> "I had 20+ business-critical queries at 4–5 seconds on 60M-row tables. I **diagnosed before changing anything** — read the execution plans, found full scans from missing indexes, N+1 patterns from the ORM, and expensive repeated joins. I added the right composite indexes, eliminated the N+1s with batch fetches, and precomputed frequently-reused static data as **materialized views** — a denormalized read model. That took response times to 300–500ms, ~90% faster. I chose the cheapest rung of the scaling ladder — query/index fixes — before reaching for caching, replicas, or sharding."

**Likely follow-ups:**
- *"Why not just add read replicas or a cache?"* → Those spread or hide load; a missing index is cheaper and fixes the root cause. Index/query first, then cache/replica/shard only if a measured bottleneck remains.
- *"How did you know it was a missing index?"* → `EXPLAIN`/execution plan showed a full table scan on a selective predicate; the composite index turned it into an index seek.
- *"Materialized views — what's the trade-off?"* → They're a denormalized copy → fast reads, but **staleness** and refresh cost; fine for static/slowly-changing reference data.
- *"When would you have sharded?"* → Only if write throughput or data size exceeded a single node after indexing/caching/replicas — not the case here.

---

## 3. Cross-System Document Sync (Hexagon EPDDMS ↔ VDMS/EDMS; retries, validation, idempotent handling, 99% accuracy, 10 days → <2 days)

**This is a distributed-integration + reliability story — idempotency and fault tolerance across systems.**

**Study:**
- [idempotency-and-deduplication](./05-messaging-and-event-driven-systems/idempotency-and-deduplication.md) — your idempotent handling; the **idempotency key + atomic check-and-record**.
- [retries-and-duplicate-execution](./06-distributed-systems/retries-and-duplicate-execution.md) — why retries need idempotency (duplicate execution).
- [idempotent-recovery-and-reconciliation](./07-reliability-and-resilience/idempotent-recovery-and-reconciliation.md) — your "99% accuracy" is really **reconciliation**; name it. This elevates the story from "retries" to "verified correctness."
- [partial-failures-and-timeouts](./06-distributed-systems/partial-failures-and-timeouts.md) + [circuit-breakers-and-bulkheads](./06-distributed-systems/circuit-breakers-and-bulkheads.md) — fault-tolerant integration across systems.
- [cloud-file-sync](./12-system-design-problems/cloud-file-sync.md) — a full sync design (your EPDDMS↔VDMS is a sync problem).
- Code: [idempotency-key-example](./15-practical-backend-examples/idempotency-key-example.md), [circuit-breaker-example](./15-practical-backend-examples/circuit-breaker-example.md), [timeout-configuration-example](./15-practical-backend-examples/timeout-configuration-example.md).

**Tell it as HLD:**
> "I synchronized documents across EPDDMS and VDMS/EDMS. Cross-system sync means **partial failures** — a request can succeed on one side and fail on the other, or time out ambiguously. I made the sync **idempotent** (keyed per document+revision), so retries after a timeout don't duplicate or corrupt. I added validation and structured exception handling, retries for transient failures, and the **99% accuracy came from reconciliation** — comparing source and target to detect and fix drift. That cut transfer+review from 10 days to under 2."

**Likely follow-ups:**
- *"How do you guarantee a document isn't synced twice?"* → Idempotency keyed per document+revision; the duplicate is a no-op.
- *"The source said success but the target write failed — now what?"* → Reconciliation detects the mismatch and re-syncs; idempotency makes the re-sync safe.
- *"Can you guarantee exactly-once sync?"* → Not at the delivery layer; **effectively-once** via idempotency + reconciliation (the honest answer).
- *"The target system is down for an hour?"* → Retries with backoff + a circuit breaker (fail fast), queue/persist pending syncs, reconcile on recovery.

---

## 4. Workflow Modules + RBAC (10+ modules: approvals, reviews, revisions, collaboration)

**Study:**
- [authentication-authorization-and-rbac](./08-security/authentication-authorization-and-rbac.md) — your RBAC; name **least privilege** and **IDOR** prevention.
- [document-management-system](./12-system-design-problems/document-management-system.md) — the DMS *is* your domain (docs, revisions, approvals, RBAC, multi-stakeholder).
- [multi-tenant-isolation](./08-security/multi-tenant-isolation.md) — if EIL serves multiple departments/projects, tenant scoping applies.
- [state machines / long-running workflows] → [saga-pattern](./05-messaging-and-event-driven-systems/saga-pattern.md) and [food-delivery-platform](./12-system-design-problems/food-delivery-platform.md) (approval workflows = state machines).

**Tell it as HLD:** "Approval/review workflows are **state machines** with role-based transitions; I enforced **resource-level authorization** (a reviewer can act only on documents in their scope) and **RBAC** per role. Revisions are versioned entities." Follow-up: *"How do you prevent a user acting on a document they shouldn't?"* → resource-level authz on every transition, not just authentication (prevents IDOR).

---

## 5. CI/CD + Failure Alerting (GitLab CI/CD, PM2, 30–45min → 5min, DB error table + email alerts)

**Study:** [deployment-strategies](./09-observability-and-operations/deployment-strategies.md) (rolling/canary, fast rollback), [logs-metrics-and-traces](./09-observability-and-operations/logs-metrics-and-traces.md) + [alerting-and-dashboards](./09-observability-and-operations/alerting-and-dashboards.md) (your error-table-to-email is basic alerting — know the upgrade path: structured logs, metrics, symptom-based alerts).

**Tell it as HLD:** "I automated deploys and added failure alerting (exceptions → error table → email)." Honest upgrade to mention if asked: "At scale I'd move to structured logs + metrics + symptom/burn-rate alerting rather than per-exception emails (alert fatigue)."

---

## 6. JobFinder Project (Spring Boot, PostgreSQL, Redis, Docker, AWS SQS; 60+ ATS boards, 3–4K listings/scan, weighted matching, incremental + idempotent ingestion, scheduled discovery, SQS async, retry/recovery, email reports)

**This is a complete personal system-design project — great to volunteer when asked "tell me about something you built."**

**Study:**
- [web-crawler](./12-system-design-problems/web-crawler.md) — your ATS-board scanning is a crawler (politeness, dedup, incremental).
- [job-scheduler](./12-system-design-problems/job-scheduler.md) + [distributed-task-queue](./12-system-design-problems/distributed-task-queue.md) — your scheduled discovery + SQS async processing.
- [idempotency-and-deduplication](./05-messaging-and-event-driven-systems/idempotency-and-deduplication.md) — your idempotent persistence.
- [caching-fundamentals](./04-caching/caching-fundamentals.md) + [redis-design-and-failure-modes](./04-caching/redis-design-and-failure-modes.md) — your Redis usage.
- [data-pipelines-and-batch-processing](./10-storage-and-content-delivery/data-pipelines-and-batch-processing.md) — your ingestion pipeline.

**Tell it as HLD:** "JobFinder ingests from 60+ ATS boards: a **scheduler** triggers discovery, **SQS** decouples ingestion from processing (absorbs spikes, retries), **idempotent persistence** dedups listings across scans, **weighted matching** ranks results, Redis caches hot data. It's an async ingestion pipeline with incremental, idempotent processing." This hits scheduling, async messaging, idempotency, caching, and dedup in one story.

---

## Your personalized readiness plan

You're ahead on the *content* for messaging, databases, idempotency, and reliability. Your gaps are likely (a) the *estimation* muscle and (b) the *consumer/scale* designs (feeds, chat, geospatial) you haven't built. Prioritize:

1. **Rehearse your 4 stories above as HLD narratives** (notification platform, Oracle optimization, EPDDMS sync, JobFinder) — these are your interview gold; interviewers love "tell me about a system you built."
2. **Drill estimation** ([estimation-cheat-sheet](./11-estimation-and-bottleneck-analysis/estimation-cheat-sheet.md)) — your resume has great numbers but HLD needs quick RPS/storage math.
3. **Do the mocks that match your strengths first** (confidence), then your gaps:
   - Strength mocks: Notification service (Mock 3), DMS (Mock 10), Job scheduler (Mock 9), File storage (Mock 8).
   - Gap mocks: Chat (Mock 4), News feed (Mock 7), Payment (Mock 5), Ticket booking (Mock 6).
4. **Study the consumer-scale designs** you *haven't* built — [chat](./12-system-design-problems/chat-messaging-system.md), [news-feed](./12-system-design-problems/news-feed.md), [ride-hailing](./12-system-design-problems/ride-hailing-system.md) — since your experience is enterprise/workflow, not consumer-scale.
5. **Target band note:** your profile (3+ yrs, strong backend, concrete impact) is a solid SDE-2 fit for ₹33 LPA+. The HLD round is about *reasoning + communication* — you have the substance; convert it with timed mocks.

**One mindset tip:** when an interviewer asks a generic design, *anchor it to what you've built* ("this is similar to the notification platform I built — same outbox + idempotency + DLQ pattern"). It's authentic, memorable, and shows real experience.

---
*Return to [README](./README.md) · start studying with [ROADMAP](./ROADMAP.md) · code examples in [15-practical-backend-examples](./15-practical-backend-examples/README.md).*
