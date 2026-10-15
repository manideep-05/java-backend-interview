# 22 — Debugging & Production Engineering

> **Priority: P0.** SDE-2 interviews lean heavily on "the service is doing X — walk me through how you'd find the cause." The point is a **disciplined process** and the right evidence, not jumping to a fix. This chapter is 12 realistic playbooks.

**Prerequisites:** [06 Concurrency](../06-java-concurrency/), [07 JVM](../07-jvm-memory-and-performance/), [12 JPA](../12-spring-data-jpa-and-hibernate/), [13 Transactions](../13-spring-transactions/), [16 Indexing](../16-database-indexing-and-query-optimization/), [17 DB transactions](../17-database-transactions-and-isolation/).

---

## A. The general workflow (use this every time)
```
1. Symptoms      — what exactly is wrong? (latency? errors? memory? which endpoints? since when?)
2. Hypotheses    — a few plausible causes, cheapest/most-likely first.
3. Evidence      — metrics, logs, traces, thread/heap dumps, EXPLAIN, DB stats. Reproduce if possible.
4. Root cause    — the evidence should point to ONE cause; avoid "fixing" symptoms.
5. Fix           — smallest correct change.
6. Verify        — reproduce the original condition and confirm it's gone under load.
7. Prevent       — test/alert/guardrail so it can't silently recur.
```
Say this out loud in interviews: **symptoms → hypotheses → evidence → root cause → fix → verify → prevent.** Never skip to step 5.

---

## C. The playbooks

### DBG-01 — An API suddenly became slow
- **Symptoms:** p99 latency up on one endpoint; CPU/DB maybe normal.
- **Hypotheses:** N+1 from a code/data change; a new slow query / missing index; downstream dependency slow; GC pauses; lock contention.
- **Evidence:** APM/trace for the endpoint (time in DB vs app vs external); SQL logs (count queries → N+1); `EXPLAIN ANALYZE` the suspect query; GC logs; thread dump.
- **Root cause (common):** a change made an association lazy-load in a loop (N+1) or dropped an index.
- **Fix:** fetch join/projection ([12](../12-spring-data-jpa-and-hibernate/)) or add/repair the index ([16](../16-database-indexing-and-query-optimization/)).
- **Verify:** query count/latency back to baseline under load. **Prevent:** assert query counts in a test; latency alert.
- **Follow-up:** "How do you know it's the DB and not the app?" → the trace's time breakdown + query count.

### DBG-02 — High CPU usage
- **Symptoms:** CPU pegged; latency up.
- **Hypotheses:** hot method (regex, serialization, tight loop), excessive GC (allocation storm), busy-wait/spin.
- **Evidence:** CPU profiler/flame graph (JFR/async-profiler) to find the hot stack; GC logs (if much time in GC, it's allocation, not compute); thread dump for spinning threads.
- **Root cause:** e.g., a catastrophic-backtracking regex or JSON serialization of huge payloads.
- **Fix:** fix the algorithm/regex, bound payloads, cache. **Verify:** CPU drops, flame graph flat. **Prevent:** load test, profiling in CI for hot paths.

### DBG-03 — Memory leak / high GC activity
- **Symptoms:** heap grows over days; full GCs frequent; eventual `OutOfMemoryError`.
- **Hypotheses:** unbounded cache/collection, `ThreadLocal` not removed on pooled threads, listeners not deregistered, classloader leak.
- **Evidence:** GC logs (old gen rising after full GC = retention); heap dump + MAT **dominator tree** to find the retainer and GC roots.
- **Root cause:** reachable-but-unneeded objects ([07](../07-jvm-memory-and-performance/)).
- **Fix:** bound the cache (Caffeine size/TTL), remove the ThreadLocal in a `finally`. **Verify:** heap stabilizes under the same load. **Prevent:** memory alert, cap caches, review ThreadLocal usage.

### DBG-04 — Database connection-pool exhaustion
- **Symptoms:** requests block/time out acquiring a connection; errors like "unable to get connection"; DB itself may be idle.
- **Hypotheses:** long transactions holding connections (remote IO inside `@Transactional`), connection leaks (not closed), pool too small for concurrency, a slow query holding connections.
- **Evidence:** HikariCP metrics (active/idle/pending/wait time); thread dump (threads parked on `getConnection`); DB `pg_stat_activity` for long-running/idle-in-transaction sessions.
- **Root cause:** often a transaction doing remote IO, or `OpenSessionInView` holding connections through rendering ([13 §8](../13-spring-transactions/), [12 §11](../12-spring-data-jpa-and-hibernate/)).
- **Fix:** move IO out of transactions, keep them short, disable OSIV, fix leaks (try-with-resources), right-size the pool. **Verify:** pending-connection count drops. **Prevent:** connection-leak detection, max-lifetime, alerts on pending.

### DBG-05 — Deadlocks / blocked requests
- **Symptoms:** some requests hang; throughput drops; `deadlock detected` / `ORA-00060` in DB logs, or Java threads BLOCKED.
- **Evidence:** `jstack` (reports Java-level deadlock cycle); DB server log and `pg_locks`/`v$lock`.
- **Root cause:** inconsistent lock/update ordering ([06 §9](../06-java-concurrency/), [17 §7](../17-database-transactions-and-isolation/)).
- **Fix:** consistent ordering (by id), shorter transactions, `tryLock`/timeouts, retry the DB victim. **Verify:** no cycles under contention test. **Prevent:** ordering convention + retry; alert on deadlock count.

### DBG-06 — Duplicate emails / notifications
- **Symptoms:** users get the same notification twice.
- **Hypotheses:** at-least-once retry without idempotency; check-then-act race ([06](../06-java-concurrency/)); sending before commit then retry; job runs on two instances.
- **Evidence:** logs/trace for two sends with the same logical id; retry/consumer config.
- **Root cause:** non-idempotent send on a retried message, or send not gated on commit.
- **Fix:** idempotent send keyed by a dedup id (unique constraint / processed-table), send in `afterCommit`, use the outbox pattern ([19](../19-messaging-and-asynchronous-processing/)). **Verify:** replay a message → one send. **Prevent:** dedup store + unique constraint.
- **Follow-up:** ties to [XT-03](../27-cross-topic-interview-scenarios/).

### DBG-07 — Duplicate document revisions under concurrency
- **Symptoms:** two concurrent edits produce an inconsistent/overwritten document.
- **Root cause:** lost update — concurrent read-modify-write without a version check.
- **Fix:** optimistic locking (`@Version`) with retry/409, or pessimistic lock for hot docs ([12 JPA-08](../12-spring-data-jpa-and-hibernate/), [17 DBTX-05](../17-database-transactions-and-isolation/)). **Verify:** concurrent edit test shows a conflict, not silent overwrite. **Prevent:** version column + conflict handling.

### DBG-08 — A Spring transaction unexpectedly didn't roll back
- **Symptoms:** an error was thrown but partial data persisted.
- **Hypotheses:** checked exception (default commits), swallowed exception, self-invocation bypassing the proxy, non-public `@Transactional`, wrong `rollbackFor`.
- **Evidence:** enable transaction + SQL logging (was there a BEGIN/ROLLBACK?); inspect the call path for self-invocation and the exception type.
- **Root cause:** one of the [TX-02](../13-spring-transactions/)/[TX-04](../13-spring-transactions/) traps.
- **Fix:** `rollbackFor`, rethrow instead of swallow, annotate the proxy entry method, make it public. **Verify:** failing path rolls back. **Prevent:** test the rollback path.

### DBG-09 — N+1 database queries
- **Symptoms:** endpoint slow; SQL logs show one query repeated with different IDs.
- **Evidence:** query count in logs / datasource-proxy / Hibernate statistics.
- **Fix:** fetch join / entity graph / projection / `@BatchSize` ([12 JPA-06](../12-spring-data-jpa-and-hibernate/)). **Verify:** 1+N → 1–2 queries. **Prevent:** query-count assertion test.

### DBG-10 — Thread-pool saturation / rising latency with queued tasks
- **Symptoms:** latency climbs; a pool's queue grows; tasks delayed.
- **Hypotheses:** unbounded queue hiding overload; pool too small for IO concurrency; downstream (DB pool) is the real limit; blocking work on the wrong pool.
- **Evidence:** pool metrics (active/queue size/rejections); thread dump (where are threads blocked?).
- **Root cause:** often an unbounded `newFixedThreadPool` queue or a pool larger than the DB can serve ([06 CON-04/CON-05](../06-java-concurrency/)).
- **Fix:** bounded queue + `CallerRunsPolicy`/backpressure; right-size to downstream capacity; separate pools for blocking IO; consider virtual threads for IO-bound. **Verify:** queue bounded, latency stable. **Prevent:** pool metrics + alerts.

### DBG-11 — Retries/timeouts causing increased load (retry storm)
- **Symptoms:** a dependency slows, then total load *increases* and everything degrades.
- **Root cause:** aggressive retries multiply load on an already-struggling dependency; synchronized retries (no jitter) cause thundering herds ([18 API-06](../18-api-engineering/)).
- **Fix:** exponential backoff **with jitter**, retry budgets/circuit breaker, idempotency so retries are safe, cap total attempts. **Verify:** dependency recovers without amplification. **Prevent:** circuit breaker + budget, load test the failure mode.

### DBG-12 — A queue accumulating messages / consumer lag
- **Symptoms:** broker backlog grows; processing falls behind.
- **Hypotheses:** consumers too slow/too few; a poison message blocking a partition; downstream slow; non-idempotent processing causing reprocessing.
- **Evidence:** consumer lag metrics; per-message processing time; DLQ contents.
- **Fix:** scale consumers / increase concurrency within ordering constraints; route poison messages to a DLQ; speed up the handler; ensure idempotency ([19](../19-messaging-and-asynchronous-processing/)). **Verify:** lag drains. **Prevent:** lag alerts, DLQ, backpressure.

### Bonus — "works locally, fails in production"
Think environment deltas: config/secrets, data volume (a query fine on 100 rows melts on 10M), concurrency (races only appear under load), network (timeouts, DNS, TLS), resource limits (container memory/CPU), and clock/timezone. Diff the environments methodically.

---

## D. Trade-offs — picking evidence tools
| Symptom | First tool | Why |
|---|---|---|
| slow endpoint | distributed trace / APM | shows where time goes (app/db/external) |
| high CPU | CPU profiler flame graph | finds the hot stack |
| memory growth | heap dump + MAT | finds the retainer |
| hang/deadlock | thread dump (jstack) | shows blocked threads/cycle |
| slow query | EXPLAIN ANALYZE | shows scan vs index, estimates |
| pool exhaustion | pool metrics + pg_stat_activity | shows who holds connections |

## E. Interview Q&A

### DBG-A (P0) — Walk me through debugging a slow endpoint.
**Strong answer:** I start with symptoms (which endpoint, since when, p50 vs p99, correlated deploy?), form a few hypotheses (N+1, slow query/missing index, slow dependency, GC, locks), then gather evidence — a trace to see whether time is in the app, DB, or external calls; SQL logs to count queries (N+1 shows as one query repeated); `EXPLAIN ANALYZE` on the suspect query; GC and thread dumps if the app itself is busy or blocked. The evidence points to one root cause; I make the smallest correct fix, verify the latency returns to baseline under realistic load, and add a guardrail (query-count test, latency alert). I don't change things speculatively.

### DBG-B (P0) — How do you distinguish an application bottleneck from a database bottleneck?
**Strong answer:** The request trace's time breakdown is decisive: if most time is in DB calls, it's the DB (slow query, N+1, lock waits, pool exhaustion) — confirmed by SQL logs and `EXPLAIN ANALYZE`/`pg_stat_activity`; if time is in app code with high CPU, a flame graph finds the hot method; if time is spent waiting with low CPU, threads are blocked on IO/locks (thread dump). I let evidence, not intuition, decide. (Ties to [JVM-05](../07-jvm-memory-and-performance/).)

### DBG-C (P1) — A transaction "succeeded" but a later step failed — what do you check?
**Strong answer:** Whether the first step's side effects were externalized before commit (e.g., a notification sent, or a `REQUIRES_NEW`/separate connection committed independently), and whether the failing step's rollback actually covered the first (self-invocation, checked-exception commit, swallowed exception). I trigger external side effects in `afterCommit` and make them idempotent so a later failure or retry can't double-apply ([13](../13-spring-transactions/), [17 DBTX-09](../17-database-transactions-and-isolation/)).

## F. Production relevance
This *is* the job at SDE-2: calm, evidence-driven incident response. Every playbook maps to a concrete fix in another chapter, but the differentiator is the process — reproduce, measure, change one thing, verify, prevent — and resisting the urge to restart/scale/tune blindly. Interviewers reward candidates who ask clarifying questions, name the exact evidence they'd collect, and distinguish "I verified" from "I assume."

## G. Revision

### 10 key takeaways
1. Always: symptoms → hypotheses → evidence → root cause → fix → verify → prevent.
2. Trace first for latency; it localizes the cost.
3. N+1 shows as one query repeated — count queries.
4. Memory growth → heap dump + dominator tree.
5. Hangs → thread dump; deadlocks reported explicitly.
6. Pool exhaustion → long tx / leaks / OSIV; check pool + pg_stat_activity.
7. Duplicates → idempotency + unique constraint + afterCommit.
8. No-rollback → checked exception / swallowed / self-invocation.
9. Retry storms → backoff+jitter + circuit breaker + budget.
10. "Works locally" → diff config, data volume, concurrency, network, limits.

### 10 rapid-fire
1. Slow endpoint first tool? (trace) 2. Leak tool? (heap dump/MAT) 3. Deadlock tool? (jstack) 4. Slow query tool? (EXPLAIN ANALYZE) 5. Pool holders? (pg_stat_activity) 6. N+1 signature? (repeated query) 7. Prevent duplicates? (idempotency+unique) 8. No rollback cause? (checked/swallowed/self-call) 9. Fix retry storm? (backoff+jitter+breaker) 10. Verify a fix? (reproduce under load)

### 5 tricky
1. Low CPU, high latency — where's the time? (waiting: IO/locks) 2. Heap grows then OOM — leak or load? (GC logs: old gen after full GC) 3. Why did scaling consumers not drain the queue? (poison msg / downstream limit) 4. Why did adding retries worsen the outage? 5. Why did a bigger pool not help? (downstream is the limit)

### 5 exercises
1. Instrument an endpoint, reproduce N+1, fix, and assert query count in a test.
2. Create a cache leak, capture a heap dump, find it in MAT, fix with Caffeine.
3. Exhaust a connection pool with a long transaction; read pool metrics; fix.
4. Trigger a no-rollback via a swallowed exception; fix and test the rollback.
5. Simulate a retry storm; add backoff+jitter + circuit breaker; show load stabilizes.

### One-page revision sheet
> **Process: symptoms→hypotheses→evidence→root cause→fix→verify→prevent.** Tools: trace(latency), flame graph(CPU), heap dump+MAT(memory), jstack(hang/deadlock), EXPLAIN ANALYZE(query), pool metrics+pg_stat_activity(pool). Common roots: N+1(repeated query→fetch join), leak(unbounded cache/ThreadLocal→bound), pool exhaustion(long tx/OSIV/leak→short tx, close), deadlock(ordering→retry), duplicates(idempotency+unique+afterCommit), no-rollback(checked/swallowed/self-call), retry storm(backoff+jitter+breaker), saturation(bounded queue+right-size). "Works locally"=env/data/concurrency/network diff.

### Prerequisites to revisit
- The fix for each playbook lives in ch.06/07/12/13/16/17/18/19.
