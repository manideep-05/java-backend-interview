# 17 — Database Transactions & Isolation

> **Priority: P0.** Isolation anomalies, lost updates, MVCC, and deadlocks are where correctness lives. Interviewers probe this because getting it wrong corrupts data silently — and because the honest answer requires distinguishing the SQL standard from what PostgreSQL and Oracle actually do.

**Prerequisites:** [15 SQL](../15-sql-and-relational-databases/), [13 Spring Transactions](../13-spring-transactions/).
**Related:** [12 JPA locking](../12-spring-data-jpa-and-hibernate/), [18 API idempotency](../18-api-engineering/), [27 scenarios](../27-cross-topic-interview-scenarios/).

---

## Table of contents
1. [ACID](#1-acid)
2. [The isolation anomalies](#2-anomalies)
3. [Isolation levels — standard vs reality](#3-levels)
4. [MVCC](#4-mvcc)
5. [Lost update & write skew](#5-lost-update)
6. [Optimistic vs pessimistic locking](#6-locking)
7. [Deadlocks](#7-deadlocks)
8. [Idempotency & unique constraints](#8-idempotency)
9. [Long transactions & pool exhaustion](#9-long-tx)
10. [Trade-offs](#d-trade-offs)
11. [Interview Q&A](#e-interview-qa)
12. [Production relevance](#f-production-relevance)
13. [Revision](#g-revision)

---

## 1. ACID

### A. Concept & intuition
- **Atomicity**: all-or-nothing. A failed transaction leaves no partial writes.
- **Consistency**: the DB moves from one valid state to another (constraints, FKs, triggers hold). This is the application+DB's job, not just the engine's.
- **Isolation**: concurrent transactions don't step on each other in ways the chosen isolation level forbids.
- **Durability**: once committed, survives a crash (WAL/redo logs).

Isolation is the one with knobs and anomalies, so it dominates interviews.

---

## 2. Anomalies

### DBTX-02 — the three standard phenomena (and two more that matter)
| Anomaly | What happens | Example |
|---|---|---|
| **Dirty read** | read another tx's *uncommitted* change | T2 reads a balance T1 wrote but then rolled back |
| **Non-repeatable read** | re-read a row, value changed by a committed tx | T1 reads row, T2 updates+commits, T1 re-reads → different |
| **Phantom read** | re-run a range query, new rows appear | T1 `SELECT count(*) WHERE status='NEW'`, T2 inserts a NEW row+commits, T1 re-runs → more rows |
| **Lost update** | two tx read-modify-write the same row; one overwrite is lost | both read balance 100, each sets +10, result 110 not 120 |
| **Write skew** | each tx reads a set, writes based on it; together they violate an invariant though neither sees a dirty value | two on-call engineers each check "someone else is on duty" then both go off duty |

Lost update and write skew are *not* in the classic three but are the ones that bite real backends.

---

## 3. Levels

### Standard mapping (ANSI SQL)
| Level | Dirty read | Non-repeatable | Phantom |
|---|---|---|---|
| READ UNCOMMITTED | possible | possible | possible |
| READ COMMITTED | prevented | possible | possible |
| REPEATABLE READ | prevented | prevented | possible (per standard) |
| SERIALIZABLE | prevented | prevented | prevented |

### Reality — this is the part interviewers reward
- **PostgreSQL** (MVCC): default **READ COMMITTED**. It has no true READ UNCOMMITTED (requesting it behaves like READ COMMITTED). Its **REPEATABLE READ is snapshot isolation** and actually **prevents phantoms** too (stronger than the standard requires), but it can still allow **write skew**. **SERIALIZABLE** uses Serializable Snapshot Isolation (SSI) and may abort transactions with a serialization failure you must retry.
- **Oracle**: default **READ COMMITTED** (statement-level snapshots). Its **SERIALIZABLE** is snapshot-based and raises `ORA-08177 "can't serialize access"` on conflict. Oracle does **not** offer REPEATABLE READ as a separate level; it does not permit dirty reads at all.

**Interview-ready phrasing:** "The ANSI levels define which anomalies are *allowed*; engines are free to prevent more. Postgres's REPEATABLE READ is snapshot isolation and blocks phantoms but not write skew; true serializability needs SERIALIZABLE, which can abort-and-retry. So I never assume levels behave identically across engines — I check the engine."

---

## 4. MVCC

### A/B. Concept & internals
**Multi-Version Concurrency Control**: instead of readers blocking writers, each write creates a new row version; readers see a consistent **snapshot** as of their statement/transaction start. This is why in Postgres and Oracle **readers don't block writers and writers don't block readers** — a major scalability property. Consequences:
- Postgres keeps old versions in the table and relies on **VACUUM** to reclaim them; long-running transactions hold back the "oldest snapshot" and cause **bloat** (an ops concern).
- Oracle uses **undo** segments to reconstruct old versions; a long read can fail with `ORA-01555 "snapshot too old"` if undo was overwritten.
- Writers still conflict on the **same row**: a second updater waits for the first to commit/rollback (row lock), then proceeds — which is where lost-update prevention and deadlocks come in.

---

## 5. Lost update

### DBTX-05 — prevent a lost update
```sql
-- The race (both at READ COMMITTED):
-- T1: SELECT balance FROM account WHERE id=1;   -- 100
-- T2: SELECT balance FROM account WHERE id=1;   -- 100
-- T1: UPDATE account SET balance=110 WHERE id=1; COMMIT;
-- T2: UPDATE account SET balance=110 WHERE id=1; COMMIT;  -- T1's +10 is lost
```
**Fix A — atomic in-place update (best when expressible in SQL):**
```sql
UPDATE account SET balance = balance + 10 WHERE id = 1; -- read-modify-write done atomically by the DB
```
**Fix B — pessimistic lock (serialize the readers):**
```sql
SELECT balance FROM account WHERE id = 1 FOR UPDATE; -- T2 blocks until T1 commits
-- compute, then UPDATE, then COMMIT
```
**Fix C — optimistic / version check (no held lock):**
```sql
UPDATE account SET balance = :new, version = version + 1
WHERE id = 1 AND version = :expected;   -- 0 rows affected => someone changed it => retry
```
**Fix D — SERIALIZABLE** (DB aborts one tx; you retry). Choose by contention and whether the change is expressible as a single UPDATE. In JPA these map to `@Version` (C) and `PESSIMISTIC_WRITE` (B) — [12 §8](../12-spring-data-jpa-and-hibernate/).

**Write skew** is *not* fixed by any of A–C on a single row; it needs SERIALIZABLE or an explicit lock on the thing the invariant is about (e.g., lock a parent row, or add a constraint).

---

## 6. Locking

- **Pessimistic**: `SELECT … FOR UPDATE` acquires a row lock; others wait. Use for frequent conflicts / must-serialize (stock, seat booking). Costs: held locks, blocking, deadlock risk, connection-pool pressure. `FOR UPDATE SKIP LOCKED` is a powerful pattern for queue-table workers (each grabs a different unlocked row).
- **Optimistic**: version column; detect conflict at write time, retry. Use for rare conflicts; scales without blocking.
- **Lock granularity**: row locks (normal) vs table locks (avoid); gap/predicate locks appear under higher isolation to stop phantoms.

---

## 7. Deadlocks

### DBTX-07 — investigation
A deadlock is a cycle of transactions each waiting on a lock the other holds (usually from inconsistent update ordering). The DB detects the cycle and **aborts one victim** with an error.
- **PostgreSQL**: logs `ERROR: deadlock detected` with the processes and the statements; check the server log / `pg_stat_activity`. Default `deadlock_timeout` ~1s before it looks for a cycle.
- **Oracle**: `ORA-00060 deadlock detected`; a trace file is written with the resource graph.

**Prevention:** update rows in a **consistent order** across the app (e.g., always by ascending id), keep transactions short, use the smallest sufficient lock, and **retry** the aborted victim (deadlocks are expected under contention, not necessarily a bug). This mirrors the Java lock-ordering fix in [06 §9](../06-java-concurrency/).

```sql
-- BROKEN ordering (deadlock-prone): different sessions lock rows in opposite order
-- S1: UPDATE acct SET ... WHERE id=1;  then WHERE id=2;
-- S2: UPDATE acct SET ... WHERE id=2;  then WHERE id=1;  -- cycle
-- FIX: always touch ids in ascending order in both paths.
```

---

## 8. Idempotency

### DBTX-09 — client times out after the DB commits
Classic distributed-systems truth: the DB can **commit** and then the response is lost (client timeout, network drop) before the client learns it succeeded. The client retries → a second insert → **duplicate record**. The DB transaction was perfectly correct; the duplication is at the request layer.

**Defenses (combine them):**
1. **Unique constraint** on the natural/business key — the DB rejects the duplicate even under a race (two concurrent inserts: one wins, the other gets a unique-violation you translate to "already created"). This is the single most reliable concurrency safeguard.
2. **Idempotency key**: client sends a unique key per logical operation; you store it and short-circuit retries ([18 §idempotency](../18-api-engineering/)).
3. **Upsert**: `INSERT … ON CONFLICT DO NOTHING/UPDATE` (Postgres) / `MERGE` (Oracle) to make the write naturally idempotent.
```sql
-- Postgres: idempotent insert
INSERT INTO payment(idem_key, amount) VALUES (:k, :amt)
ON CONFLICT (idem_key) DO NOTHING;   -- retry is safe; duplicate silently ignored
```
A unique constraint is also how you make the "two requests create duplicate records" scenario ([XT-09](../27-cross-topic-interview-scenarios/)) impossible rather than merely unlikely.

---

## 9. Long tx & pool exhaustion
A transaction holds its connection and (in MVCC) holds back the oldest snapshot. Long transactions → connection-pool exhaustion (requests block waiting for a connection — [22](../22-debugging-and-production-engineering/)), Postgres bloat / Oracle `ORA-01555`, and longer lock holds. Keep transactions short; never do remote IO inside one ([13 §8](../13-spring-transactions/)).

---

## D. Trade-offs

| Mechanism | Prevents | Cost | Use when | Poor choice when |
|---|---|---|---|---|
| READ COMMITTED (default) | dirty reads | cheapest | most OLTP | you re-read and need stability |
| REPEATABLE READ / snapshot | + non-repeatable (+ phantoms in PG) | more aborts/holds | consistent multi-read | write-skew-sensitive logic |
| SERIALIZABLE | all anomalies | serialization aborts → retries | correctness-critical invariants | high-throughput hot paths |
| Atomic `col = col + x` | lost update | none | expressible as one UPDATE | multi-step logic |
| `SELECT … FOR UPDATE` | lost update, serializes | held lock, deadlock, blocking | frequent conflict | read-mostly |
| `@Version` optimistic | lost update | retry on conflict | rare conflict | hot rows |
| Unique constraint | duplicate rows | index maintenance | always, for business keys | — (basically always want it) |
| Idempotency key | duplicate requests | storage + lookup | non-idempotent POSTs | truly idempotent ops |

---

## E. Interview Q&A

### DBTX-01 (P1) — Explain ACID; which letter is the application's responsibility?
**Strong answer:** Atomicity (all-or-nothing), Consistency (valid-state transitions via constraints/FKs — shared between app logic and DB), Isolation (concurrency behavior per level), Durability (survives crash via WAL/redo). Consistency is where the application carries the most weight: the engine enforces declared constraints, but business invariants are my code's job.

<a id="dbtx-02"></a>
### DBTX-02 (P0) — Isolation levels and the anomalies they allow — standard vs PostgreSQL/Oracle?
**Strong answer:** The ANSI levels define allowed anomalies: READ UNCOMMITTED (dirty/non-repeatable/phantom), READ COMMITTED (non-repeatable/phantom), REPEATABLE READ (phantom), SERIALIZABLE (none). But engines prevent more than required: Postgres defaults to READ COMMITTED, has no real READ UNCOMMITTED, and its REPEATABLE READ is snapshot isolation that also blocks phantoms — yet still allows write skew; SERIALIZABLE (SSI) can abort with a serialization failure to retry. Oracle defaults to READ COMMITTED, has no separate REPEATABLE READ, and SERIALIZABLE is snapshot-based raising ORA-08177 on conflict. So I never assume cross-engine equivalence; I verify.
**Weak answer:** reciting the ANSI table as if universal — the interviewer will ask "what does Postgres actually do?" and expose the gap.

### DBTX-03 (P1) — What is MVCC and what does it buy you?
**Strong answer:** Writers create new row versions; readers see a consistent snapshot, so readers don't block writers and vice versa — big concurrency win. Costs: Postgres accumulates dead tuples needing VACUUM (long transactions cause bloat); Oracle reconstructs from undo and can throw `ORA-01555` if undo is overwritten during a long read. Same-row writers still serialize via row locks.

### DBTX-04 (P1) — Difference between non-repeatable read and phantom read?
**Strong answer:** Non-repeatable read is the *same row* changing value between two reads; phantom read is the *set of rows* matching a predicate changing (new/removed rows) between two range queries. REPEATABLE READ stops the former; stopping phantoms needs REPEATABLE READ snapshot (Postgres) or SERIALIZABLE / range locks in strict engines.

<a id="dbtx-05"></a>
### DBTX-05 (P0) — How do you prevent a lost update? Give several options and pick.
**Strong answer:** (A) Express it as an atomic DB update `balance = balance + :x` so the engine does the read-modify-write under a row lock — simplest when possible. (B) Pessimistic `SELECT … FOR UPDATE` to serialize readers — for frequent conflicts. (C) Optimistic version check `WHERE version = :expected` and retry on 0 rows — for rare conflicts, no held lock. (D) SERIALIZABLE and retry on abort. I'd default to (A) when the change is a simple arithmetic update, (C) for general entity edits with rare conflict, (B) when conflicts are common or I must serialize. None of A–C fixes write skew, which needs SERIALIZABLE or a lock on the invariant's anchor.

### DBTX-06 (P1) — Optimistic vs pessimistic locking at the DB level?
**Strong answer:** Pessimistic (`FOR UPDATE`) locks up front — correct and simple for high contention but holds locks (blocking, deadlock risk, pool pressure). Optimistic (version column) holds nothing and detects conflicts at write time, retrying — scales for low contention but wastes work when conflicts are frequent. Decide by measured conflict rate. `FOR UPDATE SKIP LOCKED` is my go-to for concurrent queue workers.

<a id="dbtx-07"></a>
### DBTX-07 (P0) — How would you investigate and prevent DB deadlocks?
**Strong answer:** A deadlock is a wait cycle; the engine aborts a victim. In Postgres I read `deadlock detected` in the server log (and `pg_stat_activity`/`pg_locks` live); in Oracle it's `ORA-00060` with a trace file showing the resource graph. The usual root cause is inconsistent update ordering, so I enforce a global order (e.g., ascending id), shorten transactions, use the minimal lock, and add retry on the deadlock error since deadlocks are expected under contention. It's the same lock-ordering discipline as in Java threading.

### DBTX-08 (P1) — What is write skew and why is it sneaky?
**Strong answer:** Two transactions each read an overlapping set, make decisions, and write disjoint rows; individually valid, together they break an invariant (classic: both doctors go off-call because each saw the other on-call). No value was read dirty, so snapshot isolation doesn't catch it — you need SERIALIZABLE or an explicit lock/constraint on the invariant. It's sneaky because the usual single-row defenses (atomic update, `@Version`) don't help.

<a id="dbtx-09"></a>
### DBTX-09 (P0) — The client times out after the DB has committed, then retries. What happens and how do you prevent duplicates?
**Strong answer:** The transaction committed correctly, but the success response was lost, so the client can't tell and retries, creating a duplicate. This is a request-layer problem, not a DB bug. I defend with a **unique constraint** on the business key (the DB rejects the duplicate even under a concurrent race — one insert wins, the other gets a unique violation I translate to "already done"), an **idempotency key** so retries short-circuit, and/or an **upsert** (`ON CONFLICT DO NOTHING` / `MERGE`) to make the write naturally idempotent. The unique constraint is the backstop that makes duplicates impossible rather than unlikely.
**Follow-up:** Why not just "check if it exists first"? (check-then-insert is itself a race; two requests both see "absent" — the constraint is what actually serializes them. Mirrors the Java check-then-act bug in [06](../06-java-concurrency/).)

### DBTX-10 (P1) — Why are long transactions dangerous?
**Strong answer:** They hold a connection (pool exhaustion → request latency cascade), hold locks longer (more contention/deadlocks), and in MVCC hold back the oldest snapshot (Postgres bloat, Oracle `ORA-01555`). Keep them short and never do remote IO inside a transaction.

---

## F. Production relevance
This chapter underlies the nastiest production bugs: silent data loss (lost updates with no `@Version` or atomic update), duplicate records (retries after a committed-but-unacknowledged write), mysterious stalls (long transactions exhausting the pool, lock waits), and intermittent `deadlock detected` / `ORA-01555` errors. The durable defenses are boring and effective: unique constraints on business keys, atomic updates or version checks, short transactions, consistent lock ordering with retries, and idempotent writes. Being able to say precisely how Postgres vs Oracle differ — and that ANSI levels are a floor, not a universal spec — is a strong SDE-2 signal.

---

## G. Revision

### 10 key takeaways
1. ACID; Consistency is largely the app's job via constraints.
2. Anomalies: dirty, non-repeatable, phantom (+ lost update, write skew).
3. ANSI levels define *allowed* anomalies; engines may prevent more.
4. Postgres & Oracle default to READ COMMITTED; MVCC → readers don't block writers.
5. Postgres REPEATABLE READ = snapshot (blocks phantoms) but allows write skew.
6. SERIALIZABLE can abort → retry.
7. Prevent lost update: atomic update / FOR UPDATE / `@Version` / SERIALIZABLE.
8. Write skew needs SERIALIZABLE or a lock on the invariant.
9. Duplicates on retry → unique constraint + idempotency key + upsert.
10. Long transactions = pool exhaustion + bloat/undo + lock holds.

### 10 rapid-fire
1. PG default level? (READ COMMITTED) 2. Does PG have READ UNCOMMITTED? (no, acts as RC) 3. PG RR blocks phantoms? (yes) 4. Oracle serialize error? (ORA-08177) 5. Oracle snapshot-too-old? (ORA-01555) 6. Lock for queue workers? (FOR UPDATE SKIP LOCKED) 7. Atomic lost-update fix? (`col = col + x`) 8. Backstop for duplicates? (unique constraint) 9. What creates row versions? (MVCC writes) 10. Who reclaims PG dead tuples? (VACUUM)

### 5 tricky
1. Why doesn't `@Version` fix write skew? 2. Why is check-then-insert still a race? 3. Why can a correct commit still cause a duplicate? 4. Why did a long read fail with ORA-01555? 5. Why does Postgres RR differ from the ANSI definition?

### 5 exercises
1. Reproduce a lost update at READ COMMITTED with two sessions; fix four ways.
2. Cause a deadlock with opposite update ordering; read the log; fix with ordering + retry.
3. Make a POST idempotent with a unique constraint; prove concurrent duplicates fail cleanly.
4. Demonstrate write skew and prevent it with SERIALIZABLE (observe the serialization failure/retry).
5. Hold a long transaction open and watch connection-pool waits / Postgres bloat grow.

### One-page revision sheet
> **ACID**; Consistency=app+constraints. Anomalies: dirty/non-repeatable/phantom + **lost update/write skew**. ANSI levels = *allowed* anomalies (floor). **PG & Oracle default READ COMMITTED, MVCC** (readers≠block writers). PG RR=snapshot (no phantoms) but write skew possible; SERIALIZABLE aborts→retry (SSI / ORA-08177). Lost update fix: `col=col+x` / FOR UPDATE / `@Version` / SERIALIZABLE. Write skew → SERIALIZABLE or explicit lock. Dup on retry → **unique constraint** (+ idempotency key + upsert `ON CONFLICT`/`MERGE`). FOR UPDATE SKIP LOCKED = queue workers. Long tx → pool exhaustion + bloat/undo (ORA-01555). Deadlock → consistent order + retry.

### Prerequisites to revisit
- SQL basics & constraints (ch.15).
- Spring transaction boundaries & propagation (ch.13); JPA locking (ch.12).
- Java lock-ordering/deadlock parallel (ch.06); API idempotency (ch.18).
