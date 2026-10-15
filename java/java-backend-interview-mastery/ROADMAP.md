# Learning Roadmap

A 10-phase path from object semantics to mock interviews. Each phase lists prerequisites, objectives, priority-ordered topics, must-master questions, exercises, a time estimate, completion criteria, and a revision checkpoint.

> Time estimates assume ~3 years of Java experience and ~8–10 focused study hours per week. They are planning aids, not promises. Adjust to your gaps.

---

## Phase 1 — Core Java & object semantics
- **Prerequisites:** working Java syntax; ability to compile and run with Maven.
- **Objectives:** reason precisely about references, equality, immutability, and the String pool; never fall for the `Integer` cache `==` trap.
- **Topics (priority order):** pass-by-value & references → `equals`/`hashCode` contract → immutability & defensive copying → String pool & `String`/`StringBuilder` → wrapper caching → records/enums/sealed.
- **Must-master questions:** CJ-01 (`==` vs `equals`), CJ-04 (equals/hashCode contract), CJ-07 (why String is immutable), CJ-09 (mutable key in a HashMap).
- **Exercises:** implement a correct immutable `Money` value type; write a class safe to use as a `HashMap` key; demonstrate the `Integer` cache boundary.
- **Estimate:** ~1 week.
- **Completion criteria:** explain, from memory, why a broken `equals`/`hashCode` loses entries in a `HashMap`, and fix it.
- **Revision checkpoint:** R1 (end of week 1).

## Phase 2 — Collections, generics, exceptions, modern Java
- **Prerequisites:** Phase 1.
- **Objectives:** choose the right collection and justify it; read generic signatures with PECS; design exceptions and error responses; use streams/`Optional` without misuse.
- **Topics:** HashMap internals (hash, buckets, resize, treeify) → ArrayList vs LinkedList → fail-fast iterators → ConcurrentHashMap (preview before Phase 3) → generics & type erasure → checked vs unchecked & try-with-resources → streams, `Optional`, `CompletableFuture` basics.
- **Must-master questions:** COL-01 (why HashMap isn't thread-safe), COL-03 (collision handling), COL-06 (fail-fast), GEN-02 (PECS), EXC-02 (try-with-resources & suppressed), MJ-03 (map vs flatMap).
- **Exercises:** cause and then fix a `ConcurrentModificationException`; capacity-plan a `HashMap`; rewrite a nested loop as a stream and then argue when the loop was clearer.
- **Estimate:** ~2 weeks.
- **Completion criteria:** defend a collection choice under follow-ups; explain type erasure consequences.
- **Revision checkpoint:** R2.

## Phase 3 — Concurrency, JVM, performance
- **Prerequisites:** Phases 1–2.
- **Objectives:** reason about atomicity/visibility/ordering; pick the right synchronization primitive; size thread pools; investigate a slow/leaky JVM with evidence.
- **Topics:** race conditions & JMM/happens-before → `synchronized` & `volatile` (and its limits) → atomics/CAS → locks & conditions → `ExecutorService`, pool sizing, saturation, rejection → `CompletableFuture` → virtual threads → GC fundamentals → leaks, heap/thread dumps, GC logs → benchmarking pitfalls.
- **Must-master questions:** CON-01 (volatile insufficient for `i++`), CON-04 (why bigger pool can be slower), CON-07 (deadlock), JVM-02 (leak investigation), JVM-05 (CPU-bound vs IO-bound).
- **Exercises:** break a counter, fix with `AtomicLong` and with a lock, compare; build a bounded producer/consumer; reproduce a classic lock-ordering deadlock and resolve it.
- **Estimate:** ~3 weeks (densest phase).
- **Completion criteria:** given a slow-service scenario, lay out a disciplined investigation without jumping to a fix.
- **Revision checkpoint:** R3 (mid) and R4 (end).

## Phase 4 — Spring Core, Boot, MVC
- **Prerequisites:** Phases 1–3.
- **Objectives:** explain DI and proxying; trace a request end to end; separate DTOs from entities; centralize error handling.
- **Topics:** IoC & DI → bean lifecycle/scopes → component scan vs `@Bean` → circular deps → proxies & AOP → auto-configuration → request lifecycle (filter → DispatcherServlet → controller → service → repo) → validation → `@ControllerAdvice`.
- **Must-master questions:** SC-01 (how DI works internally), SC-04 (constructor vs field injection), SB-02 (which auto-config made a bean), MVC-03 (filter vs interceptor), MVC-05 (DTO vs entity).
- **Exercises:** build a 3-layer endpoint with validation and a global exception handler; debug an ambiguous bean with `@ConditionalOn...`.
- **Estimate:** ~2 weeks.
- **Completion criteria:** draw the request lifecycle from memory and name where each cross-cutting concern attaches.
- **Revision checkpoint:** R5.

## Phase 5 — JPA/Hibernate & transactions
- **Prerequisites:** Phase 4 + Phase 6 SQL basics can run in parallel.
- **Objectives:** reason about the persistence context; find and fix N+1; explain `@Transactional` proxy semantics and self-invocation; pick locking strategy.
- **Topics:** entity states → persistence context & dirty checking → flush vs commit → persist vs merge → lazy/eager → N+1 & fetch joins/entity graphs/projections → optimistic vs pessimistic locking → `@Transactional` internals → propagation & isolation → rollback rules → self-invocation → async thread boundary.
- **Must-master questions:** JPA-03 (flush vs commit), JPA-05 (how N+1 arises), JPA-08 (optimistic vs pessimistic), TX-02 (self-invocation failure), TX-04 (rollback & checked exceptions), TX-06 (why a tx doesn't cross threads).
- **Exercises:** reproduce N+1 in logs, fix three ways, compare; cause a silent no-rollback and fix it; implement optimistic locking with `@Version` and handle the retry.
- **Estimate:** ~3 weeks.
- **Completion criteria:** explain why an annotation that *looks* correct does nothing (self-invocation / non-public / wrong exception).
- **Revision checkpoint:** R6.

## Phase 6 — SQL, indexing, optimization, DB correctness
- **Prerequisites:** Phase 1; can run alongside Phases 4–5.
- **Objectives:** write non-trivial SQL; reason about index usage from a plan; explain isolation anomalies and prevent lost updates.
- **Topics:** joins/subqueries/CTEs → window & ranking functions → NULL/three-valued logic → keyset vs OFFSET pagination → B-tree & composite index column order → covering indexes & sargability → execution plans & statistics → ACID & isolation levels → dirty/non-repeatable/phantom, lost update, write skew → MVCC → optimistic/pessimistic locking → deadlocks → idempotency & unique constraints.
- **Must-master questions:** SQL-04 (EXISTS vs IN), SQL-07 (window function), IDX-02 (why a composite index is ignored), IDX-05 (sargability), DBTX-02 (isolation anomalies), DBTX-05 (prevent lost update), DBTX-07 (deadlock investigation).
- **Exercises:** optimize a slow query and *verify* with the plan; demonstrate a lost update at READ COMMITTED and prevent it two ways; write keyset pagination.
- **Estimate:** ~3 weeks.
- **Completion criteria:** read an `EXPLAIN ANALYZE` and explain why an index was or wasn't used.
- **Revision checkpoint:** R7.

## Phase 7 — API engineering, security, messaging, caching
- **Prerequisites:** Phases 4–6.
- **Objectives:** design idempotent, versioned APIs resilient to retries; apply authn/authz correctly; build idempotent consumers; cache without breaking correctness.
- **Topics:** REST & HTTP methods/idempotency → status codes & error design → idempotency keys vs idempotent methods → retries/backoff/retry budgets → pagination/filtering/versioning → authn vs authz, JWT vs sessions → delivery guarantees & idempotent consumers → DLQ/outbox → cache-aside/write-through → invalidation, stampede, stale data.
- **Must-master questions:** API-03 (idempotent method vs idempotency key), API-06 (retries worsening an outage), SEC-02 (JWT mistakes), MSG-03 (at-least-once + idempotent consumer), CACHE-04 (caching causing correctness bugs).
- **Exercises:** add an idempotency-key filter to a POST; implement cache-aside with correct invalidation; design a DLQ flow for a poison message.
- **Estimate:** ~2 weeks.
- **Revision checkpoint:** R8.

## Phase 8 — Testing, networking, production debugging, reliability
- **Prerequisites:** Phases 1–7.
- **Objectives:** test at the right level; explain HTTP/TLS basics; run disciplined incident investigations; reason about SLOs and failure isolation.
- **Topics:** unit/integration/e2e → Mockito doubles → `@WebMvcTest` vs `@SpringBootTest` → Testcontainers → testing async/flaky → DNS/TCP/TLS/HTTP2/keep-alive/timeouts → the production-debugging playbooks (ch.22) → metrics/logs/traces, timeouts, circuit breakers.
- **Must-master questions:** TEST-03 (`@WebMvcTest` vs `@SpringBootTest`), NET-02 (TLS handshake), DBG-01..12 (the incident playbooks).
- **Exercises:** write a Testcontainers-backed repository test; walk through two incident playbooks end to end.
- **Estimate:** ~2 weeks.
- **Revision checkpoint:** R9.

## Phase 9 — Mixed-topic interview scenarios
- **Prerequisites:** Phases 1–8.
- **Objectives:** connect Java + Spring + SQL + concurrency + ops under one prompt.
- **Topics:** all 12 cross-topic scenarios in ch.27.
- **Exercises:** answer each scenario out loud, timed, then compare to the polished answer.
- **Estimate:** ~1 week.
- **Revision checkpoint:** R10.

## Phase 10 — Mock interviews & final revision
- **Prerequisites:** Phases 1–9.
- **Objectives:** perform under interview conditions; close remaining gaps.
- **Topics:** all 8 mock rounds (ch.28) + practice mode; then `REVISION_CHECKLIST.md`.
- **Completion criteria:** every Tier-0 topic at *Mastered*; all P0 questions answerable with follow-ups.
- **Revision checkpoint:** final readiness checklist.

---

## Spaced revision schedule
| Checkpoint | When | Covers |
|---|---|---|
| R1 | end of Phase 1 | core Java |
| R2 | end of Phase 2 | collections/generics/exceptions/modern Java |
| R3/R4 | within/after Phase 3 | concurrency & JVM |
| R5 | end of Phase 4 | Spring core/Boot/MVC |
| R6 | end of Phase 5 | JPA & transactions |
| R7 | end of Phase 6 | SQL & DB correctness |
| R8 | end of Phase 7 | API/security/messaging/caching |
| R9 | end of Phase 8 | testing/networking/ops |
| R10 | end of Phase 9 | cross-topic |
| Final | Phase 10 | everything |

Revisit any topic marked *Needs Revision* at the next two checkpoints, not just the next one.
