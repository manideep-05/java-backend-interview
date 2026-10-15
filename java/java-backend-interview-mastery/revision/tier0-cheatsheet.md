# Tier-0 Cheat Sheet

The 10 topics you must not walk in without. One tight block each. Full detail in the linked chapter.

### 1. Concurrency ([06](../06-java-concurrency/))
Three problems: **atomicity / visibility / ordering**. `volatile` = visibility+ordering, NOT atomic compound ops; `i++` is RMW → `AtomicLong`/lock. Happens-before: program order, monitor unlock→lock, volatile write→read, start, join, final-field, transitive. Pools grow past core **only when queue full** → bound the queue, `CallerRunsPolicy`; size = cores (CPU) / downstream capacity (IO). `CompletableFuture`: own executor, handle errors, don't block request thread. Virtual threads: IO-bound scale, no CPU gain, beware pinning, one-per-task. Deadlock → consistent lock order + tryLock/timeout.

### 2. Spring transactions ([13](../13-spring-transactions/))
`@Transactional` = **proxy + thread-bound tx**. Bypassed by **self-invocation** and non-public methods. Rollback default: **unchecked only**; checked **commits** unless `rollbackFor`; swallowed exception commits. Propagation: REQUIRED(join)/REQUIRES_NEW(independent, separate conn)/NESTED(savepoint). `readOnly`=hint. Tx **doesn't cross threads** (@Async) → pass ids, own tx, `afterCommit` for side effects. Long tx → pool exhaustion.

### 3. JPA / Hibernate ([12](../12-spring-data-jpa-and-hibernate/))
Persistence context = per-tx 1st-level cache; managed entities **dirty-checked** (update without `save()`). **Flush = emit SQL; commit = durable.** `@ManyToOne` EAGER, collections LAZY. **N+1 = 1+N queries** → fetch join (no collection pagination) / entity graph / DTO projection / `@BatchSize`. Optimistic(`@Version`, no lock, retry) vs pessimistic(`FOR UPDATE`). Disable OSIV; fetch in service.

### 4. Collections / HashMap ([03](../03-collections-framework/))
put: spread `h^(h>>>16)` → `(n-1)&h` → chain → treeify(8 & ≥64 buckets, Java 8+); resize at 0.75 load. equals⇒same hashCode. Not thread-safe (lost updates + resize corruption). ConcurrentHashMap: per-bin, no null, **single-op atomic only** → use `computeIfAbsent`/`merge`. Fail-fast = modCount detector (use `removeIf`).

### 5. DB transactions & isolation ([17](../17-database-transactions-and-isolation/))
Anomalies: dirty / non-repeatable / phantom + **lost update / write skew**. ANSI levels = *allowed* anomalies (floor). **PG & Oracle default READ COMMITTED, MVCC.** PG RR = snapshot (blocks phantoms) but write skew possible; SERIALIZABLE aborts→retry. Lost update: `col=col+x` / FOR UPDATE / `@Version` / SERIALIZABLE. Dup on retry → **unique constraint** + idempotency key + upsert. Deadlock → consistent order + retry.

### 6. SQL + indexing ([15](../15-sql-and-relational-databases/)/[16](../16-database-indexing-and-query-optimization/))
LEFT join: right-col predicate in **ON**, not WHERE. NULL=unknown → `IS NULL`, `NOT EXISTS` not `NOT IN`. Window funcs: top-N/running total/dedup (per-row over partition). Keyset > OFFSET deep paging. **Composite index = left-prefix only; equality cols first, range last.** Sargable = bare-column compare (no function/cast/leading `%`). Stale stats → bad plans. **Always EXPLAIN ANALYZE.**

### 7. Core Java ([01](../01-core-java/))
Never `==` wrappers (Integer cache −128..127). **Pass-by-value** (object value = reference). `==` identity vs `equals`. **equals⇒equal hashCode**; break it → lost map/set entries. Immutable = final + private final + defensive copies / immutable field types. String immutable + pooled; compare with `equals`. Loop concat → `StringBuilder`.

### 8. Production debugging ([22](../22-debugging-and-production-engineering/))
**symptoms → hypotheses → evidence → root cause → fix → verify → prevent.** Trace(latency), flame graph(CPU), heap dump+MAT(memory), jstack(hang/deadlock), EXPLAIN ANALYZE(query), pool metrics+pg_stat_activity(pool). DB vs app = trace time breakdown. Don't skip to the fix.

### 9. Spring Core DI ([09](../09-spring-core-and-dependency-injection/))
IoC+DI; container builds BeanDefinitions → inject → **BeanPostProcessors create proxies** (@Transactional/@Async/@Cacheable) → @PostConstruct. **Constructor injection default** (final, explicit, testable, fail-fast). Self-invocation bypasses proxies. Spring singleton = per-container → **stateless**.

### 10. REST API lifecycle & correctness ([11](../11-spring-mvc-and-rest-apis/)/[18](../18-api-engineering/))
filters → DispatcherServlet → controller → service(tx) → repo → converter. **One request = one thread.** **DTOs at boundary, never entities.** Errors → @ControllerAdvice + ProblemDetail. **Idempotency key (mechanism) ≠ idempotent method (semantics)**; back the key with a unique constraint. Retries = backoff+jitter+budget+breaker, idempotent only. Timeout every call.
