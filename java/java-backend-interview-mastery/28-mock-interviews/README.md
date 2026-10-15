# 28 — Mock Interviews

> **Priority: P0.** Knowing material isn't the same as performing under pressure. This chapter has 8 realistic SDE-2 rounds plus a **practice mode** (questions first, answer key separate). Each round models the real flow: clarify → answer → follow-up → improve → edge case → exercise → rubric.

**How to use:**
1. Do a round **timed** (~45 min), out loud, before reading the ideal answers.
2. Score yourself with the rubric.
3. For gaps, revisit the linked chapter and mark it *Needs Revision* in [`PROGRESS.md`](../PROGRESS.md).
4. Use [Practice mode](#practice-mode) to drill questions without answers, then check the [answer key](#answer-key).

The ideal answers are intentionally pointer-style — the full reasoning lives in each chapter's §E, cross-referenced by question ID.

---

## Evaluation rubric (apply to every answer)
| Dimension | Weak (1) | Strong (3) |
|---|---|---|
| Clarify | dives in | asks about scale/consistency/failure first |
| Correctness | slogans | precise, distinguishes guarantee vs impl |
| Depth | one layer | survives 2+ follow-ups |
| Trade-offs | one option | compares, picks, justifies |
| Evidence | assumes | names what to measure/verify |
| Communication | rambles | structured, concise |

Target: mostly 3s on P0 topics before interviewing.

---

## Round 1 — Core Java & Collections
1. **Q:** `Integer a=127,b=127; a==b`? And `128`? Why? → *clarify:* none needed. *Ideal:* [CJ-01](../01-core-java/) — Integer cache −128..127; use `equals`.
   - **Follow-up:** Why is `new String("x") == "x"` false? → [CJ-07](../01-core-java/).
   - **Edge case:** Unboxing a null `Integer`? → NPE.
2. **Q:** Walk a `HashMap.put`. → [COL-02](../03-collections-framework/) hash→bucket→chain→treeify(8/≥64)→resize(0.75).
   - **Follow-up:** Why not thread-safe? → [COL-01](../03-collections-framework/).
   - **Follow-up:** Does `ConcurrentHashMap` make `containsKey`+`put` atomic? → No, [COL-03](../03-collections-framework/); use `computeIfAbsent`.
3. **Exercise:** Fix a class that loses entries in a `HashMap` (missing `hashCode`). → [ch.01 §C](../01-core-java/).

---

## Round 2 — Concurrency & JVM
1. **Q:** Why is `volatile` insufficient for a counter? → [CON-01](../06-java-concurrency/).
   - **Follow-up:** `AtomicLong` vs `LongAdder` vs lock? → [ch.06 §6/§D](../06-java-concurrency/).
2. **Q:** Why can a bigger thread pool be slower? → [CON-04](../06-java-concurrency/).
   - **Follow-up:** Thread-pool saturation & rejection policies? → [CON-05](../06-java-concurrency/).
   - **Edge case:** `newFixedThreadPool`'s hidden risk? → unbounded queue.
3. **Q:** How would you investigate a production memory leak? → [JVM-02](../07-jvm-memory-and-performance/) (GC logs → heap dump → dominator tree).
   - **Follow-up:** CPU-bound vs IO-bound — how to tell? → [JVM-05](../07-jvm-memory-and-performance/).
4. **Exercise:** Reproduce a lock-ordering deadlock and fix with ordering + retry. → [CON-07](../06-java-concurrency/).

---

## Round 3 — Spring Boot & Transactions
1. **Q:** What does `@Transactional` do under the hood? → [TX-01](../13-spring-transactions/) (proxy + thread-bound tx).
   - **Follow-up:** Why does self-invocation break it? → [TX-02](../13-spring-transactions/).
   - **Follow-up:** A method threw but data persisted — why? → [TX-04](../13-spring-transactions/) (checked exception / swallowed).
2. **Q:** Why doesn't a transaction extend to an `@Async` thread? → [TX-06](../13-spring-transactions/).
   - **Edge case:** Where do you safely send a post-commit notification? → `afterCommit`.
3. **Q:** How do you debug which auto-configured bean was created? → [SB-02](../10-spring-boot-internals/).
4. **Exercise:** Show `@Transactional` silently not rolling back; fix two ways. → [DBG-08](../22-debugging-and-production-engineering/).

---

## Round 4 — JPA / Hibernate
1. **Q:** Flush vs commit? → [JPA-03](../12-spring-data-jpa-and-hibernate/).
2. **Q:** How does N+1 arise and how do you fix it? → [JPA-05/JPA-06](../12-spring-data-jpa-and-hibernate/).
   - **Follow-up:** Why can't you `join fetch` a collection with `Pageable`? → in-memory pagination.
3. **Q:** Optimistic vs pessimistic locking — when each? → [JPA-08](../12-spring-data-jpa-and-hibernate/).
   - **Edge case:** Entity `equals`/`hashCode` with generated ids? → [JPA-09](../12-spring-data-jpa-and-hibernate/).
4. **Exercise:** Reproduce N+1 in logs, fix with a projection, assert the query count. → [DBG-09](../22-debugging-and-production-engineering/).

---

## Round 5 — SQL & Query Optimization
1. **Q:** Write second-highest salary per department. → [SQL-07](../15-sql-and-relational-databases/) (DENSE_RANK window).
2. **Q:** Why might a correct-looking composite index be ignored? → [IDX-02](../16-database-indexing-and-query-optimization/).
   - **Follow-up:** What is sargability? Give non-sargable predicates. → [IDX-05](../16-database-indexing-and-query-optimization/).
3. **Q:** EXISTS vs IN and the NULL trap? → [SQL-04](../15-sql-and-relational-databases/).
4. **Exercise:** Optimize a slow query and verify with `EXPLAIN ANALYZE`. → [IDX-07](../16-database-indexing-and-query-optimization/).

---

## Round 6 — API Engineering & Security
1. **Q:** Idempotent HTTP method vs idempotency key? → [API-03](../18-api-engineering/).
   - **Follow-up:** Why back the key with a unique constraint? → [XT-09](../27-cross-topic-interview-scenarios/).
2. **Q:** Why can retries make an outage worse, and how do you fix it? → [API-06](../18-api-engineering/) (backoff+jitter+budget+breaker).
3. **Q:** Common JWT mistakes? → [SEC-02](../14-spring-security/).
   - **Follow-up:** CORS vs CSRF; when disable CSRF? → [SEC-04](../14-spring-security/).
4. **Q:** How do you prevent SQL injection and IDOR? → [SEC25-01/SEC25-03](../25-backend-security/).
5. **Exercise:** Add idempotency-key handling to a POST. → [ch.18 §C](../18-api-engineering/).

---

## Round 7 — Production Debugging
1. **Q:** An endpoint is suddenly slow — walk me through it. → [DBG-A](../22-debugging-and-production-engineering/).
   - **Follow-up:** DB vs app bottleneck — how do you tell? → [DBG-B](../22-debugging-and-production-engineering/).
2. **Q:** Connection-pool exhaustion with an idle DB — why? → [XT-04](../27-cross-topic-interview-scenarios/).
3. **Q:** Duplicate notifications after a retry — root cause and fix? → [XT-03](../27-cross-topic-interview-scenarios/).
4. **Edge case:** A queue keeps growing — hypotheses? → [DBG-12](../22-debugging-and-production-engineering/).

---

## Round 8 — Mixed backend engineering
A single flowing scenario (the interviewer escalates):
1. "Design the write path for creating a payment that must never double-charge on client retries." → idempotency key + unique constraint + transaction + afterCommit + outbox ([XT-09](../27-cross-topic-interview-scenarios/), [18](../18-api-engineering/), [19](../19-messaging-and-asynchronous-processing/)).
2. "Now two app instances run a reconciliation job — it ran twice." → distributed lock + idempotency ([XT-10](../27-cross-topic-interview-scenarios/)).
3. "Under load the endpoint is slow but CPU is low." → waiting on DB/locks/pool; trace + thread dump ([XT-12](../27-cross-topic-interview-scenarios/)).
4. "The query was fast in staging." → data volume / index / stats ([XT-07](../27-cross-topic-interview-scenarios/)).
5. "A cached balance is stale after a write." → after-commit eviction / distributed cache ([XT-08](../27-cross-topic-interview-scenarios/)).
**What's tested:** can you move fluidly across concurrency, transactions, SQL, caching, and ops under one narrative, always clarifying and reasoning from evidence.

---

## Practice mode

Cover the answer key. Give yourself ~3–5 minutes per question, out loud, clarifying first. Then check the key.

### Practice questions (no answers here)
P1. Trace `HashMap.put` and explain treeification thresholds.
P2. Why is `volatile count++` buggy and what are two correct fixes?
P3. Explain happens-before and give an example of two unordered actions.
P4. Why can increasing a thread pool reduce throughput?
P5. What does `@Transactional` do, and name three ways it silently does nothing.
P6. Checked vs unchecked exceptions and Spring's default rollback behavior.
P7. Flush vs commit; why did an UPDATE fire without `save()`?
P8. How does N+1 arise; compare three fixes.
P9. Optimistic vs pessimistic locking — pick for a hot stock-decrement row.
P10. Map each isolation level to the anomalies it allows; what does Postgres actually do?
P11. Prevent a lost update — four options, and which you'd pick.
P12. Why might a composite index be ignored? Define sargability.
P13. EXISTS vs IN, and the NULL pitfall.
P14. Idempotent method vs idempotency key; why a unique constraint backs it.
P15. Why do retries sometimes worsen an outage? How do you make them safe?
P16. Build an idempotent message consumer; why is it necessary?
P17. How can caching introduce correctness bugs, and how do you invalidate correctly?
P18. `@WebMvcTest` vs `@SpringBootTest`; when does mocking hide a bug?
P19. Investigate a memory leak step by step.
P20. Low CPU, high latency — where's the time and how do you confirm?

### Answer key
- **P1:** [COL-02](../03-collections-framework/) — spread hash → `(n-1)&h` → chain → treeify at 8 with ≥64 buckets (Java 8+) → resize at 0.75 load.
- **P2:** [CON-01](../06-java-concurrency/) — RMW not atomic; fix with `AtomicLong.incrementAndGet()` or a lock.
- **P3:** [CON-02](../06-java-concurrency/) — hb is a visibility/ordering guarantee; two plain-field accesses with no lock/volatile are unordered.
- **P4:** [CON-04](../06-java-concurrency/) — context switches (CPU-bound) / downstream limit (IO-bound) / lock contention.
- **P5:** [TX-01/TX-02/TX-04/TX-06](../13-spring-transactions/) — proxy+thread-bound tx; broken by self-invocation, non-public method, checked exception/swallowed, async thread.
- **P6:** [EXC-01](../05-exceptions-and-resource-management/)/[TX-04](../13-spring-transactions/) — unchecked rolls back by default, checked commits unless `rollbackFor`.
- **P7:** [JPA-03](../12-spring-data-jpa-and-hibernate/) — flush emits SQL, commit makes durable; managed-entity dirty checking updates without `save()`.
- **P8:** [JPA-05/06](../12-spring-data-jpa-and-hibernate/) — 1+N; fetch join / entity graph / projection / `@BatchSize`.
- **P9:** [JPA-08](../12-spring-data-jpa-and-hibernate/) — hot contended row → pessimistic `FOR UPDATE` (or atomic `stock = stock - 1 WHERE stock > 0`).
- **P10:** [DBTX-02](../17-database-transactions-and-isolation/) — ANSI table + Postgres defaults READ COMMITTED, RR=snapshot (blocks phantoms, allows write skew), SERIALIZABLE aborts.
- **P11:** [DBTX-05](../17-database-transactions-and-isolation/) — atomic update / FOR UPDATE / `@Version` / SERIALIZABLE.
- **P12:** [IDX-02/IDX-05](../16-database-indexing-and-query-optimization/) — left-prefix/column order/stats/selectivity; sargable = bare-column comparison.
- **P13:** [SQL-04](../15-sql-and-relational-databases/) — EXISTS short-circuits & NULL-safe; `NOT IN` + NULL returns nothing.
- **P14:** [API-03](../18-api-engineering/)/[XT-09](../27-cross-topic-interview-scenarios/) — method semantics vs dedup mechanism; constraint serializes concurrent retries.
- **P15:** [API-06](../18-api-engineering/) — retry storms; backoff+jitter+budget+breaker+idempotency.
- **P16:** [MSG-03](../19-messaging-and-asynchronous-processing/) — dedup by message id + unique constraint in same tx; needed because at-least-once redelivers.
- **P17:** [CACHE-04](../20-caching-and-performance/) — stale copies; evict after commit (DB-then-cache), bound TTL, distributed for multi-node.
- **P18:** [TEST-03/TEST-02](../21-testing-and-code-quality/) — slice vs full context; mocking the DB hides real SQL/constraint bugs.
- **P19:** [JVM-02](../07-jvm-memory-and-performance/) — GC logs confirm → heap dump + dominator tree → fix roots → verify.
- **P20:** [XT-12](../27-cross-topic-interview-scenarios/)/[JVM-05](../07-jvm-memory-and-performance/) — waiting (DB/locks/pool); thread dump + trace.

---

## Final readiness check
You're ready when, across Rounds 1–8, you consistently: clarify before answering, state guarantees precisely, survive two follow-ups, give trade-offs with a pick, and name the evidence you'd gather. Then run [`REVISION_CHECKLIST.md`](../REVISION_CHECKLIST.md).
