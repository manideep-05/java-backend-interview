# 27 — Cross-Topic Interview Scenarios

> **Priority: P0.** The hardest SDE-2 questions connect Java + Spring + SQL + concurrency + ops under one prompt. Each scenario below forces you to combine concepts rather than recite a definition. For each: the prompt, clarifying questions to ask, step-by-step investigation, root causes, the correct implementation, alternatives/trade-offs, failure cases, follow-ups, and a polished answer.

**Use the general debugging workflow from [ch.22](../22-debugging-and-production-engineering/):** symptoms → hypotheses → evidence → root cause → fix → verify → prevent. **Always clarify before answering** (scale, consistency needs, failure tolerance) — that itself is a strong signal.

Related chapters are linked per scenario.

---

## XT-01 — Two users submit different revisions of the same document simultaneously
**Prompt:** How do you prevent inconsistent state when concurrent edits race?
**Clarify:** How often do conflicts happen? Is last-writer-wins acceptable, or must we detect conflicts? Single DB or distributed? Can we show the user a conflict?
**Investigation:** Reproduce two concurrent read-modify-write on version N; observe one overwrite lost ([17 DBTX-05](../17-database-transactions-and-isolation/), [12 JPA-08](../12-spring-data-jpa-and-hibernate/)).
**Root cause:** Lost update — no concurrency control on the row.
**Correct implementation:** Optimistic locking with `@Version`; the second save fails `OptimisticLockException` → return 409 or merge/retry. If conflicts are frequent or must serialize, pessimistic `SELECT … FOR UPDATE`.
**Alternatives/trade-offs:** Optimistic (no locks, scales, retries on conflict) vs pessimistic (serializes, held locks, deadlock/pool risk). Last-writer-wins is simplest but silently loses data.
**Failure cases:** retry loops under high contention; write skew if the invariant spans rows (needs SERIALIZABLE/explicit lock).
**Follow-ups:** How do you surface the conflict to the user? How does this interact with idempotent retries?
**Polished answer:** "I'd start with optimistic locking via a `@Version` column so a concurrent edit fails fast with a 409 the UI can turn into a merge prompt — it holds no locks and scales. If conflict rates are high or the operation must serialize, I'd switch to a pessimistic row lock, accepting the contention cost. I'd confirm the race in a test and never rely on last-writer-wins, which loses data silently."

---

## XT-02 — A Spring Boot endpoint takes 5s though its SQL query takes 100ms
**Prompt:** Where did the other 4.9s go?
**Clarify:** Is it one query or many? Latency since a deploy? p50 or p99? Payload size?
**Investigation:** Trace the request (app vs DB vs external); **count queries** in SQL logs — 1 fast query repeated 1000× is the tell ([12 JPA-05](../12-spring-data-jpa-and-hibernate/), [DBG-09](../22-debugging-and-production-engineering/)).
**Root cause:** N+1 — lazy associations loaded in a loop, often during JSON serialization because the controller returned entities ([11 MVC-05](../11-spring-mvc-and-rest-apis/)).
**Correct implementation:** Fetch join / entity graph / DTO projection; return DTOs, not entities; disable OSIV.
**Alternatives/trade-offs:** fetch join (one query, no collection pagination), projection (minimal columns, read-only), `@BatchSize` (IN batches).
**Failure cases:** fetch join + pagination = in-memory paging; eager fetch = cartesian blowup.
**Follow-ups:** How do you know it's the DB and not GC/external? (trace breakdown + query count). Prevent recurrence? (query-count assertion test).
**Polished answer:** "A single 100ms query means the time is elsewhere. I'd trace the request to split app vs DB time and count queries — the classic cause is N+1 from lazy associations serialized in the controller. I'd fix it with a DTO projection or fetch join, return DTOs instead of entities, disable OSIV, and add a test asserting the query count so it can't regress."

---

## XT-03 — An email/notification job sends duplicates after a retry
**Prompt:** Why duplicates, and how do you stop them?
**Clarify:** At-least-once delivery? Is the send idempotent? Is it sent inside or after the transaction? One instance or many?
**Investigation:** Logs show two sends for one logical id after a redelivery/retry ([19 MSG-03](../19-messaging-and-asynchronous-processing/), [DBG-06](../22-debugging-and-production-engineering/)).
**Root cause:** Non-idempotent send on at-least-once redelivery, and/or sending before commit then retrying, and/or a check-then-act race ([06](../06-java-concurrency/)).
**Correct implementation:** Idempotent send keyed by a dedup id backed by a unique constraint (a `sent_notifications` table); send in `afterCommit`; use the outbox pattern so the DB change and the "to-send" record are atomic.
**Alternatives/trade-offs:** dedup table (simple, DB-enforced) vs provider-side idempotency keys; outbox adds a relay but removes the dual-write gap.
**Failure cases:** sending before commit (tx rolls back but email already sent); two instances both sending (needs the dedup constraint to serialize).
**Follow-ups:** What if the email provider itself retries? (provider idempotency key). Exactly-once? (no — effectively-once via dedup).
**Polished answer:** "At-least-once delivery means retries, so the send must be idempotent: I record a unique dedup key per notification with a unique constraint and skip if it already exists, send only in `afterCommit` so a rolled-back transaction can't email, and use the outbox pattern to avoid the dual-write gap. Two concurrent instances are serialized by the unique constraint."

---

## XT-04 — Database connection-pool exhaustion during traffic spikes
**Prompt:** Requests hang acquiring connections; the DB looks idle. Why?
**Clarify:** Pool size vs request concurrency? Long transactions? OSIV on? Any remote calls inside `@Transactional`?
**Investigation:** HikariCP metrics (pending/active, wait time); thread dump (threads parked on `getConnection`); `pg_stat_activity` for idle-in-transaction ([13 §8](../13-spring-transactions/), [DBG-04](../22-debugging-and-production-engineering/)).
**Root cause:** Transactions held open across slow IO (remote calls, OSIV holding through rendering), or connection leaks, or a pool sized below concurrency.
**Correct implementation:** Keep transactions short, move remote IO outside the transaction, disable OSIV, fix leaks with try-with-resources, right-size the pool to the DB's capacity (not arbitrarily large — [06 CON-04](../06-java-concurrency/)).
**Alternatives/trade-offs:** bigger pool (bounded by DB max connections — just moves the bottleneck); backpressure/429 when saturated.
**Failure cases:** a bigger pool overwhelming the DB; readiness probe depending on the pool and flapping.
**Follow-ups:** Why doesn't more connections fix it? (DB max connections / CPU is the real limit).
**Polished answer:** "I'd check pool metrics and a thread dump — threads parked on connection acquisition point to connections held too long, usually a transaction spanning remote IO or OSIV. The fix is short transactions with IO outside the boundary, OSIV off, leak detection, and sizing the pool to what the DB can actually serve. Blindly enlarging the pool just pushes the bottleneck onto the database."

---

## XT-05 — A transaction appears to succeed but a later operation fails
**Prompt:** The first step persisted though a later step failed. Why, and how do you make it consistent?
**Clarify:** Are the steps in one transaction? Any `@Async`/`REQUIRES_NEW`? Checked exceptions? External side effects?
**Investigation:** Enable tx + SQL logging (was there a ROLLBACK?); inspect for self-invocation, checked exceptions, swallowed catches, async boundaries ([13 TX-02/TX-04/TX-06](../13-spring-transactions/), [DBG-08](../22-debugging-and-production-engineering/)).
**Root cause:** The two steps weren't in the same atomic unit — self-invocation bypassed the proxy, a checked exception committed, an exception was swallowed, or an external side effect fired before commit.
**Correct implementation:** Single transaction on the proxy entry method, `rollbackFor` where needed, don't swallow, trigger external effects in `afterCommit`, make them idempotent.
**Alternatives/trade-offs:** saga/compensation if the steps genuinely span services (can't share a local tx).
**Failure cases:** `REQUIRES_NEW` committing independently; client timeout after commit ([XT-09]).
**Polished answer:** "I'd confirm with transaction logging whether a rollback actually happened. The usual causes are the step not being in the transaction at all — self-invocation, a checked exception that commits by default, or a swallowed exception — or an external effect sent before commit. I'd put both steps under one transaction on the proxy entry method, add `rollbackFor` as needed, and move external side effects into an `afterCommit` hook made idempotent."

---

## XT-06 — A thread pool has many queued tasks and rising latency
**Prompt:** Tasks pile up and latency climbs. Diagnose and fix.
**Clarify:** Bounded or unbounded queue? CPU vs IO-bound work? What's downstream? Pool size?
**Investigation:** Pool metrics (queue size, active, rejections); thread dump (blocked where?) ([06 CON-04/CON-05](../06-java-concurrency/), [DBG-10](../22-debugging-and-production-engineering/)).
**Root cause:** Unbounded queue hiding overload, or pool too small/large relative to the real bottleneck (often a downstream DB pool), or blocking work on a shared pool.
**Correct implementation:** Bounded queue + `CallerRunsPolicy`/backpressure; size to downstream capacity; separate pools per workload (bulkhead); consider virtual threads for IO-bound.
**Alternatives/trade-offs:** more threads (helps IO only up to downstream limit; hurts CPU-bound); backpressure vs dropping.
**Failure cases:** unbounded queue → OOM/latency blowup; pool >> DB pool → queue on the DB.
**Polished answer:** "I'd look at pool metrics and a thread dump. A growing unbounded queue is masking overload; the fix is a bounded queue with caller-runs backpressure, sizing the pool to what the downstream can serve rather than guessing high, and isolating blocking IO on its own pool — or using virtual threads if it's IO-bound. Adding threads only helps until the real bottleneck, usually the DB, is saturated."

---

## XT-07 — A query is fast in dev but slow in production
**Prompt:** Same query, different performance. Why?
**Clarify:** Data volume difference? Index present in prod? Statistics fresh? Parameter values (skew)? Concurrent load/locks?
**Investigation:** `EXPLAIN ANALYZE` in prod vs dev; compare plans (seq scan vs index), estimated vs actual rows ([16 IDX-02/IDX-06](../16-database-indexing-and-query-optimization/), [DBG-01](../22-debugging-and-production-engineering/)).
**Root cause:** Dev has 100 rows (any plan is fast); prod has 10M where a missing/mis-ordered index, a non-sargable predicate, or stale statistics forces a scan; or lock contention under concurrent load.
**Correct implementation:** Add/repair the index with the right column order, make predicates sargable, refresh statistics; test against production-like volume.
**Alternatives/trade-offs:** covering index (faster reads, more write cost); query rewrite.
**Failure cases:** parameter sniffing/skew producing a bad cached plan; stale stats after bulk load.
**Polished answer:** "Dev data is tiny, so any plan looks fast. In prod I'd run `EXPLAIN ANALYZE` and compare plans — the usual culprits are a missing or wrongly-ordered index, a non-sargable predicate, or stale statistics causing a sequential scan on millions of rows. I fix the index/predicate, refresh stats, and always validate against production-scale data, not a dev table."

---

## XT-08 — A cache returns stale document metadata after an update
**Prompt:** Users see old metadata after a write. Why, and how do you fix it correctly?
**Clarify:** Local or distributed cache? TTL-only or invalidate-on-write? Invalidate before or after commit? Multiple instances?
**Investigation:** Trace a read right after a write; check invalidation ordering and whether per-node local caches diverge ([20 CACHE-04](../20-caching-and-performance/), [DBG — stale cache]).
**Root cause:** Wrong invalidation ordering (evicted before commit, then a concurrent read repopulated stale data), TTL-only staleness, or per-node local caches not sharing invalidation.
**Correct implementation:** Evict **after commit** (DB-then-cache); prefer eviction over caching uncommitted state; for multi-instance consistency use a distributed cache or a shared invalidation signal; bound staleness with TTL.
**Alternatives/trade-offs:** write-through (always fresh, slower writes) vs cache-aside + after-commit eviction; local (fast, divergent) vs distributed (consistent, network cost).
**Failure cases:** repopulation race if evicting pre-commit; local caches on N nodes each stale.
**Polished answer:** "Stale reads after a write are almost always an invalidation ordering or multi-node issue. I evict after the DB commit (DB-then-cache) so a concurrent read can't repopulate pre-commit data, prefer eviction over caching uncommitted values, and for multiple instances use a distributed cache or a shared invalidation event rather than per-node local caches. TTL bounds the worst-case staleness."

---

## XT-09 — An endpoint times out, the client retries, and duplicate records are created
**Prompt:** Why duplicates, and how do you make the write safe to retry?
**Clarify:** Is the operation a POST (non-idempotent)? Does the client send an idempotency key? Is there a unique business key?
**Investigation:** The DB committed before the response reached the client; the client's retry inserted again ([17 DBTX-09](../17-database-transactions-and-isolation/), [18 API-03](../18-api-engineering/)).
**Root cause:** Non-idempotent POST + retry after a committed-but-unacknowledged write; a check-then-insert is itself a race.
**Correct implementation:** Idempotency key stored and enforced by a unique constraint (so concurrent duplicates fail cleanly), or a unique constraint on the business key, or an upsert (`ON CONFLICT`/`MERGE`).
**Alternatives/trade-offs:** idempotency key (client cooperation) vs natural unique key (always works); check-then-insert is NOT sufficient (race).
**Failure cases:** two concurrent retries both passing a `SELECT` existence check — only the constraint serializes them.
**Follow-ups:** Why not just check existence first? (race). Idempotent method vs key? ([API-03](../18-api-engineering/)).
**Polished answer:** "The transaction committed, but the success response was lost, so the client retried and created a second row — a request-layer problem, not a DB bug. I make the write idempotent: an idempotency key or natural business key enforced by a unique constraint so even two concurrent retries can't both insert, or an upsert. A pre-check isn't enough because two requests can both see 'absent' — the constraint is what actually serializes them."

---

## XT-10 — A scheduled job runs twice on different instances
**Prompt:** A `@Scheduled` task double-executes across a cluster. Why, and how do you fix it?
**Clarify:** How many instances? Is double execution harmful (non-idempotent)? Need exactly-one or just at-most-one-at-a-time?
**Investigation:** `@Scheduled` runs on **every** instance by default; logs show two runs ([19](../19-messaging-and-asynchronous-processing/)).
**Root cause:** No coordination — each instance's scheduler fires independently.
**Correct implementation:** Distributed lock (ShedLock, DB advisory lock) or leader election so one instance runs it; and make the job idempotent as defense in depth.
**Alternatives/trade-offs:** distributed lock (simple) vs a dedicated scheduler/quartz cluster vs leader election; idempotency makes double-runs harmless.
**Failure cases:** lock not released on crash (use TTL/lease); clock skew.
**Polished answer:** "By default every instance runs the schedule, so in a cluster it double-fires. I'd gate it with a distributed lock like ShedLock (or a DB advisory lock / leader election) so only one instance executes each tick, and I'd still make the job idempotent so a missed-lock edge case doesn't cause harm."

---

## XT-11 — A bulk import partially succeeds and then fails
**Prompt:** Half the rows imported, then an error. What's the right behavior and implementation?
**Clarify:** All-or-nothing or best-effort per item? Is re-running safe (idempotent)? How large? Memory limits?
**Investigation:** Was it one transaction (all rolled back?) or committed in chunks (partial persisted)? ([13](../13-spring-transactions/), [18 API-04](../18-api-engineering/), [12 batching](../12-spring-data-jpa-and-hibernate/)).
**Root cause:** Undefined partial-failure contract and/or chunked commits leaving some rows persisted.
**Correct implementation:** Decide the contract explicitly — all-or-nothing in one transaction (simple, but one bad row fails everything and large tx strains the DB) or per-item/per-chunk with a result report and idempotent upserts so re-running skips already-imported rows. For large volumes, chunk with batching + flush/clear to bound memory, record progress, and make it resumable.
**Alternatives/trade-offs:** single tx (atomic, memory/lock pressure) vs chunked (resumable, needs idempotency and a progress record).
**Failure cases:** huge single transaction (locks, pool, memory); non-idempotent re-run duplicating rows.
**Polished answer:** "First I'd pin down the contract: all-or-nothing or best-effort per item. For a large import I prefer idempotent, chunked processing with batching and a progress/dedup record, so a failure can be retried and already-imported rows are skipped via upsert — rather than one giant transaction that strains locks and memory. I'd return a per-item/per-chunk report of what succeeded and failed."

---

## XT-12 — Low CPU usage but high response times
**Prompt:** The service is slow but CPU is low. Where's the time going?
**Clarify:** p99 vs p50? Which endpoints? Any external calls/DB? Thread states?
**Investigation:** Thread dump (threads WAITING/BLOCKED on IO or locks); trace (time in DB/external/locks) ([07 JVM-05](../07-jvm-memory-and-performance/), [DBG-B](../22-debugging-and-production-engineering/)).
**Root cause:** The service is **waiting**, not computing — slow DB/external calls, lock contention, connection-pool waits, or GC pauses (if GC, CPU may spike).
**Correct implementation:** Follow the trace to the waiting resource: fix the slow query/N+1, add timeouts/circuit breakers to external calls, reduce lock contention, right-size pools; adding threads won't help if the limit is downstream.
**Alternatives/trade-offs:** virtual threads for IO-bound blocking; caching to cut DB round trips.
**Failure cases:** mistaking an IO-bound wait for a CPU problem and adding threads.
**Polished answer:** "Low CPU with high latency means we're waiting, not computing. A thread dump and a trace tell me where — usually slow DB/external calls, lock contention, or connection-pool waits. I fix the specific wait (query/N+1, add timeouts and a circuit breaker to the external call, reduce contention, right-size the pool). Adding threads is the wrong instinct here because the bottleneck is downstream, not CPU."

---

## How to practice these
For each scenario: cover the answer, read only the prompt, speak your clarifying questions and investigation out loud (timed, ~5 min), then compare to the polished answer. Grade yourself on: did you clarify first? did you reason from evidence? did you give the trade-off? See [ch.28 mock interviews](../28-mock-interviews/).
