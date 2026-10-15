# 13 — Spring Transaction Management

> **Priority: P0.** `@Transactional` *looks* trivial — one annotation — which is exactly why interviewers love it. The depth lives in **why a correct-looking annotation does nothing**: proxy self-invocation, non-public methods, swallowed exceptions, wrong propagation, and the thread boundary. If you can explain those, you demonstrate you understand Spring's proxy model and JDBC/JPA transaction binding, not just the API.

**Prerequisites:** [09 Spring Core](../09-spring-core-and-dependency-injection/) (proxies, bean lifecycle), [12 JPA/Hibernate](../12-spring-data-jpa-and-hibernate/) (persistence context), [17 DB transactions](../17-database-transactions-and-isolation/) (isolation, locking).

---

## Table of contents
1. [What a transaction is & why Spring manages it](#1-concept)
2. [How `@Transactional` works internally](#2-internals)
3. [The failure modes (self-invocation, visibility, exceptions)](#3-failure-modes)
4. [Propagation](#4-propagation)
5. [Isolation & read-only](#5-isolation--read-only)
6. [Rollback rules](#6-rollback-rules)
7. [Transactions and threads / async](#7-threads--async)
8. [Long transactions & connection-pool pressure](#8-long-tx)
9. [Distributed transactions](#9-distributed)
10. [Trade-offs](#d-trade-offs)
11. [Interview Q&A](#e-interview-qa)
12. [Production relevance](#f-production-relevance)
13. [Revision](#g-revision)

---

## 1. Concept

### A. Concept & intuition
A transaction groups DB operations into an all-or-nothing unit (ACID — see [17](../17-database-transactions-and-isolation/)). Without framework support you'd write boilerplate: get a connection, `setAutoCommit(false)`, try { … `commit()` } catch { `rollback()` } finally { close }. That boilerplate is error-prone (forgotten rollback, leaked connection) and tangles business logic with transaction plumbing.

Spring's **declarative transaction management** (`@Transactional`) factors that out: you declare the boundary, Spring supplies the begin/commit/rollback/cleanup around your method via a proxy and a `PlatformTransactionManager`. The problem it solves is *consistent, correct, non-boilerplate transaction demarcation*.

**What breaks without it:** partial writes left committed after a mid-method failure; leaked connections; inconsistent isolation; business code polluted with try/catch/finally.

---

## 2. Internals

### B. How `@Transactional` works — step by step
1. At startup, Spring sees `@Transactional` on a bean's class/method and wraps the bean in a **proxy** (JDK dynamic proxy if it implements an interface, else a CGLIB subclass).
2. When *another* object calls the method **through the proxy**, a `TransactionInterceptor` runs first.
3. The interceptor asks the `PlatformTransactionManager` (e.g., `DataSourceTransactionManager` for JDBC, `JpaTransactionManager` for JPA) to **begin** a transaction according to propagation/isolation: it obtains a connection, sets `autoCommit=false`, and binds the connection/`EntityManager` to the **current thread** (via `TransactionSynchronizationManager`, a `ThreadLocal`).
4. Your method body runs. Repositories on the same thread reuse the bound connection/`EntityManager` — that's how they all join the same transaction.
5. On normal return, the interceptor **commits** (which for JPA triggers a flush of pending changes — see [12 flush vs commit](../12-spring-data-jpa-and-hibernate/)).
6. On a matching exception (unchecked by default), it **rolls back**.
7. Finally it unbinds and returns the connection to the pool.

**The one-sentence model to say in an interview:** "`@Transactional` is proxy-based advice that begins a thread-bound transaction before the method and commits/rolls back after it — so anything that bypasses the proxy or leaves the thread also bypasses the transaction."

---

## 3. Failure modes

### C. Broken → fixed, the classics

#### (a) Self-invocation bypasses the proxy — TX-02
```java
// BROKEN: internal call does NOT start a transaction
@Service
class OrderService {
    public void placeOrder(Order o) {
        this.saveWithTx(o);          // self-call: goes direct to 'this', NOT through the proxy
    }
    @Transactional
    public void saveWithTx(Order o) { /* writes */ } // annotation ignored here
}
```
Why: the proxy wraps the *bean reference others hold*. `this.saveWithTx()` calls the raw object, so the interceptor never runs. No transaction; a mid-method failure leaves partial writes committed.

```java
// FIXED option 1: put @Transactional on the entry method that's called through the proxy
@Transactional
public void placeOrder(Order o) { save(o); }

// FIXED option 2: split into two beans so the call crosses a proxy boundary
// FIXED option 3 (last resort): self-inject the proxy or use TransactionTemplate
```

#### (b) Non-public method
`@Transactional` on `private`/`protected`/package-private methods is **ignored** by the default proxy approach (CGLIB can't advise private; Spring's default only applies to public methods). Make the transactional method `public`.

#### (c) Swallowed exception → no rollback
```java
// BROKEN: catch-and-ignore means Spring sees a normal return -> COMMIT of partial work
@Transactional
public void process() {
    writeA();
    try { writeB(); } catch (Exception e) { log.warn("ignored", e); } // tx still commits writeA
}
```
Fix: rethrow, or call `TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()`, or don't catch.

#### (d) Checked exception doesn't roll back by default — see §6.

---

## 4. Propagation

Propagation decides what happens when a transactional method is called while a transaction may already exist.

| Propagation | If a tx exists | If none exists | Typical use |
|---|---|---|---|
| **REQUIRED** (default) | join it | start one | 95% of cases |
| **REQUIRES_NEW** | suspend outer, start a new independent tx | start one | must commit independently (e.g., audit/outbox log that should persist even if the outer rolls back) |
| **NESTED** | savepoint within the outer | start one | partial rollback to a savepoint (JDBC savepoints; DB-dependent) |
| **SUPPORTS** | join it | run non-transactionally | read that's fine either way |
| **NOT_SUPPORTED** | suspend it | run non-transactionally | long non-tx work |
| **MANDATORY** | join it | throw | must be called within a tx |
| **NEVER** | throw | run non-transactionally | must not be in a tx |

**REQUIRES_NEW gotcha:** it uses a **separate connection**. If the outer tx holds locks the inner needs, you can self-deadlock on the connection pool. Also, `REQUIRES_NEW` only takes effect across a proxy boundary (same self-invocation caveat).

```java
// Audit entry that must survive even if the business tx rolls back
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void writeAudit(AuditEvent e) { auditRepo.save(e); }
```

---

## 5. Isolation & read-only

- **`isolation`**: maps to the DB isolation level for the transaction (DEFAULT uses the DB default — READ COMMITTED on PostgreSQL and Oracle). Overriding it is uncommon; know the anomalies ([17](../17-database-transactions-and-isolation/)). Not all levels are honored by all drivers/DBs identically — it's **configuration/engine-dependent**, not a universal guarantee.
- **`readOnly = true`**: a *hint*. For JPA/Hibernate it sets the `FlushMode` to `MANUAL`/`NEVER` so Hibernate skips dirty-checking flushes (a real optimization) and may set the JDBC connection read-only (driver-dependent). It does **not** forcibly prevent writes at the DB level in general — don't rely on it as a security control.

---

## 6. Rollback rules

### TX-04 — the default that surprises everyone
By default, Spring rolls back on **`RuntimeException` and `Error` (unchecked)** and **commits** on **checked exceptions**. So:
```java
// BROKEN expectation: this COMMITS despite throwing, because IOException is checked
@Transactional
public void save() throws java.io.IOException {
    repo.save(x);
    throw new java.io.IOException("boom"); // checked -> Spring commits the save!
}
```
```java
// FIXED: tell Spring to roll back on this checked type
@Transactional(rollbackFor = java.io.IOException.class)
public void save() throws java.io.IOException { ... }
```
Rationale (historical, from EJB): checked exceptions were considered recoverable "business" outcomes. You can tune with `rollbackFor` / `noRollbackFor`. **Say this clearly** — it's a top trap.

Also: once a transaction is marked rollback-only (e.g., an inner `REQUIRED` method failed), the outer commit throws `UnexpectedRollbackException` — a confusing symptom worth naming.

---

## 7. Threads & async

### TX-06 — a transaction does not cross threads
The transaction (connection + `EntityManager`) is bound to the **current thread** via `ThreadLocal`. If you start a new thread, submit to an executor, or call an `@Async` method, that work runs on a **different thread with no bound transaction**:
```java
// BROKEN: @Async method runs on another thread -> no inherited tx, lazy loads fail, writes aren't in caller's tx
@Transactional
public void handle(Long id) {
    Order o = repo.findById(id).orElseThrow();
    asyncService.doLater(o);   // @Async: runs elsewhere; 'o' is now detached, lazy fields fail
}
```
Fixes: make the async method **start its own** `@Transactional`; pass IDs/DTOs, not entities; re-fetch inside the new transaction. (Cross-ref [06 §13](../06-java-concurrency/).) Likewise, Spring's transaction synchronization callbacks (`afterCommit`) are the right place to trigger side effects that must only happen if the tx actually committed — e.g., publishing an event or sending a notification (prevents the "committed but client timed out / duplicate" class of bugs; see [27](../27-cross-topic-interview-scenarios/)).

---

## 8. Long tx & connection-pool pressure

A transaction holds a DB connection for its whole duration. Long methods (remote calls, large loops, user think-time) inside `@Transactional` keep connections checked out → the pool (e.g., HikariCP, default 10) exhausts under load → requests block waiting for a connection → cascading latency (a classic incident, [22](../22-debugging-and-production-engineering/)). Rules of thumb:
- Keep transactions **short**; do remote/IO calls **outside** the transaction.
- Don't wrap an entire controller flow in one transaction.
- Watch for `@Transactional` around methods that call external services.

---

## 9. Distributed transactions

Spring supports JTA/XA for two-phase commit across resources, but XA is operationally heavy, locks resources across a network, and scales poorly. Modern practice for cross-service consistency prefers **sagas** (compensating actions) and the **transactional outbox** ([19](../19-messaging-and-asynchronous-processing/)): write the business change and an outbox row in **one local transaction**, then a relay publishes the message — giving at-least-once delivery with eventual consistency instead of distributed locking. Interview framing: "I avoid distributed transactions where I can; I make the write + intent atomic locally and reconcile asynchronously."

---

## D. Trade-offs

| Choice | Pros | Cons | When | Poor choice when |
|---|---|---|---|---|
| Declarative `@Transactional` | clean, no boilerplate | proxy caveats (self-invocation, non-public) | almost always | you need fine-grained programmatic control inside loops |
| `TransactionTemplate` (programmatic) | explicit boundaries, no proxy issue | more code | self-invocation cases, per-iteration tx | simple service methods |
| REQUIRED | simple, joins outer | inner failure dooms whole tx | default | independent sub-op must persist |
| REQUIRES_NEW | independent commit | separate connection, deadlock risk, overhead | audit/outbox | high-frequency inner calls |
| readOnly hint | skips flush, driver read-only | not a hard write barrier | pure reads | relying on it for safety |
| XA/2PC | strong cross-resource atomicity | slow, locks, ops burden | legacy must-have | microservices at scale |
| Outbox/saga | scalable, resilient | eventual consistency, dedup needed | cross-service | you truly need immediate atomicity |

---

## E. Interview Q&A

### TX-01 (P0) — What does `@Transactional` actually do under the hood?
**Strong answer:** It's AOP: Spring creates a proxy around the bean; a `TransactionInterceptor` runs before/after the method, asking a `PlatformTransactionManager` to begin a transaction (obtain a connection, `autoCommit=false`, bind it to the thread), then commit on normal return or roll back on an unchecked exception, then release the connection. Repositories on the same thread reuse the bound connection, which is how they share the transaction.
**Weak answer:** "It makes the method a transaction." Doesn't show the proxy/thread-binding model, so you can't explain the failure modes.

<a id="tx-02"></a>
### TX-02 (P0) — Why does `@Transactional` fail on self-invocation?
**Strong answer:** The annotation is honored by a proxy that wraps the bean. A self-call (`this.method()`) invokes the raw object directly, bypassing the proxy and its interceptor, so no transaction starts. Fixes: annotate the externally-called entry method, split into two beans so the call crosses the proxy, self-inject the proxy, or use `TransactionTemplate`. The same reasoning explains why `@Transactional` on non-public methods is ignored.
**Follow-up:** How would you *prove* it's happening? (Enable transaction/SQL logging and see no BEGIN; or notice partial commits.)

### TX-03 (P1) — REQUIRED vs REQUIRES_NEW vs NESTED?
**Strong answer:** REQUIRED (default) joins an existing tx or starts one — one atomic unit. REQUIRES_NEW suspends the outer and runs in an independent tx on a **separate connection**, committing/rolling back on its own — use for audit/outbox that must persist even if the outer rolls back, but beware connection-pool self-deadlock. NESTED uses a savepoint inside the outer so you can roll back part of the work (DB/driver-dependent). I'd use REQUIRES_NEW sparingly because of the extra connection and ordering hazards.

<a id="tx-04"></a>
### TX-04 (P0) — When does a transaction roll back? Why might a thrown exception still commit?
**Strong answer:** By default Spring rolls back on unchecked (`RuntimeException`/`Error`) and **commits** on checked exceptions — a historical EJB convention. So throwing a checked `IOException` from a `@Transactional` method commits the prior writes unless I add `rollbackFor`. Also, catching an exception and not rethrowing makes Spring see a normal return and commit; to force rollback from within I'd call `setRollbackOnly()`. These two — checked-exception default and swallowed exceptions — are the usual reasons a "failed" method still persisted data.

### TX-05 (P1) — What does `readOnly = true` do?
**Strong answer:** It's an optimization hint: for Hibernate it switches flush mode so dirty-checking flushes are skipped (less work, no accidental writes), and it may mark the JDBC connection read-only depending on the driver. It is **not** a hard guarantee that writes are rejected at the DB, so I don't treat it as a security boundary — just a performance/intent signal for read paths.

<a id="tx-06"></a>
### TX-06 (P0) — Why doesn't a transaction extend to another thread (e.g., `@Async`)?
**Strong answer:** Spring binds the connection and `EntityManager` to the current thread via `ThreadLocal` (`TransactionSynchronizationManager`). A new thread/executor/`@Async` call has its own thread with nothing bound, so it runs outside the caller's transaction: lazy-loaded entities passed across fail (detached), and writes aren't part of the caller's atomic unit. The fix is to give the async work its own `@Transactional`, pass IDs/DTOs instead of entities, and re-fetch inside the new transaction. For side effects that must only fire after a successful commit, I hook `afterCommit` synchronization.
**Follow-up:** How does this connect to duplicate notifications? (If you send before commit and the tx rolls back, or re-send on retry across threads without idempotency — see XT-03.)

### TX-07 (P1) — Why can `@Transactional` methods cause connection-pool exhaustion?
**Strong answer:** A transaction holds its connection for its entire duration. If the method does slow IO (remote calls, big loops) inside the transaction, connections stay checked out; under load the pool empties and new requests block waiting for a connection, so latency cascades even though the DB itself is fine. Fix: keep transactions short, move remote/IO calls outside the boundary, and don't wrap whole request flows.

### TX-08 (P2) — What's `UnexpectedRollbackException`?
**Strong answer:** When an inner REQUIRED method fails, it marks the shared transaction rollback-only. The outer method may catch the inner exception and try to continue/commit, but at commit Spring refuses and throws `UnexpectedRollbackException` because the tx was already doomed. It's a sign of swallowing an exception from a joined inner transaction.

---

## F. Production relevance
Transaction mistakes cause silent data corruption (partial commits from self-invocation or swallowed/checked exceptions), outages (connection-pool exhaustion from long transactions), and duplicate side effects (sending notifications before commit or across threads). The safe defaults: short REQUIRED transactions on public entry methods, remote IO outside the boundary, explicit `rollbackFor` when you use checked exceptions, `afterCommit` hooks for external side effects, and the outbox pattern instead of distributed transactions. Being able to say *why an annotation did nothing* is a strong SDE-2 signal because it proves you understand Spring's proxy model and JDBC/JPA thread binding.

---

## G. Revision

### 10 key takeaways
1. `@Transactional` = proxy advice around a **thread-bound** transaction.
2. Self-invocation and non-public methods bypass the proxy → no tx.
3. Default rollback: unchecked only; checked exceptions **commit** unless `rollbackFor`.
4. Swallowing an exception commits partial work; use `setRollbackOnly()`.
5. REQUIRED joins; REQUIRES_NEW is an independent tx on a separate connection.
6. `readOnly` is an optimization hint, not a write barrier.
7. Transactions don't cross threads/`@Async`; pass IDs, re-fetch, own tx.
8. Trigger external side effects in `afterCommit`.
9. Long transactions exhaust the connection pool; keep them short, IO outside.
10. Prefer outbox/saga over XA for cross-service consistency.

### 10 rapid-fire
1. Default propagation? (REQUIRED) 2. Default isolation? (DB default, usually READ COMMITTED) 3. Rolls back on checked? (no, unless rollbackFor) 4. Does `this.method()` start a tx? (no) 5. Private @Transactional? (ignored) 6. Does @Async inherit the tx? (no) 7. REQUIRES_NEW connection? (separate) 8. readOnly a guarantee? (no, a hint) 9. Where to send email safely? (afterCommit) 10. Cross-service alternative to 2PC? (outbox/saga)

### 5 tricky
1. Why did a method that threw still save data? (checked exception OR swallowed) 2. Why did REQUIRES_NEW deadlock? (separate connection vs locks held by outer) 3. Why `UnexpectedRollbackException`? (joined inner tx marked rollback-only) 4. Why did a lazy field fail in an @Async method? (no tx/EntityManager on that thread) 5. Why did the pool exhaust though the DB was idle? (long tx holding connections during IO).

### 5 exercises
1. Reproduce self-invocation silently skipping a tx; prove it with SQL logging; fix two ways.
2. Throw a checked exception and show the data committed; add `rollbackFor` and show rollback.
3. Pass a lazy-loaded entity into an `@Async` method; observe failure; fix by passing an id + own tx.
4. Wrap a slow remote call inside `@Transactional`; load-test to exhaust the pool; move IO outside.
5. Implement `afterCommit` to send a notification only on successful commit.

### One-page revision sheet
> **@Transactional = proxy + thread-bound tx.** Bypassed by self-invocation & non-public methods. Begin→(run)→commit(flush)/rollback→release. Rollback default: unchecked only; checked **commits** unless `rollbackFor`; swallowed exception commits → `setRollbackOnly()`. Propagation: REQUIRED(join), REQUIRES_NEW(independent, separate conn, deadlock risk), NESTED(savepoint), MANDATORY/NEVER/SUPPORTS/NOT_SUPPORTED. `readOnly`=hint (skips Hibernate flush). Tx ≠ cross-thread: @Async has no tx → pass ids, own @Transactional, afterCommit for side effects. Long tx → pool exhaustion (keep short, IO outside). Cross-service: outbox/saga over XA.

### Prerequisites to revisit
- Spring proxies & bean lifecycle (ch.09).
- Persistence context, flush vs commit (ch.12).
- Isolation levels & locking (ch.17); thread-binding (ch.06).
