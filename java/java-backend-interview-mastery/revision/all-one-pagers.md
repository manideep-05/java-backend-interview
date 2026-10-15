# All One-Pagers — night-before sweep

Every chapter's one-page revision sheet, in order. Read only this file the night before. Learn nothing new. For the condensed Tier-0 set, use [`tier0-cheatsheet.md`](./tier0-cheatsheet.md).

> Each sheet is the authoritative "§G one-page revision sheet" from its chapter. If a line is unclear, open the chapter (linked) — don't try to re-derive it at 11pm.

---

## 01 Core Java → [chapter](../01-core-java/)
JDK⊃JRE⊃JVM; `javac`→bytecode→JVM(interpret+JIT). **Wrappers: never `==`** (cache −128..127); unboxing null→NPE. **Pass-by-value** (object value = reference): mutate-through yes, reassign no. `==`=identity, `equals`=logical. **equals⇒equal hashCode**; break it → lost map/set entries. Immutable: final class + private final + defensive copies / immutable field types; thread-safe, safe key. `final`≠deep immutable. **String immutable + pool**; literals interned; `new String` distinct; compare with equals. Loop concat → `StringBuilder`. Records=DTO/value; enum=closed set/singleton; sealed=closed hierarchy.

## 02 OOP → [chapter](../02-oop-and-java-design-fundamentals/)
Pillars: encapsulation/abstraction/inheritance/polymorphism. **Overloading=static bind; overriding=dynamic dispatch.** Fields & statics NOT polymorphic. **Favor composition over inheritance** (fragile base class). Interface=contract(many, default/static, no state); abstract=shared state+skeleton(single). Program to interfaces → DI/testable. Don't call overridable methods in constructors. Init: super→fields→body; static once. Covariant returns. Default methods=interface evolution.

## 03 Collections → [chapter](../03-collections-framework/)
**ArrayList/HashMap = defaults.** HashMap put: spread `h^(h>>>16)` → `(n-1)&h` → chain → treeify(8 & ≥64 buckets, Java 8+). Resize at 0.75 load, doubles. equals⇒same hashCode. Not thread-safe (lost updates + resize corruption). ConcurrentHashMap: per-bin, no null, single-op atomic only → use `computeIfAbsent`/`merge`/`putIfAbsent`. TreeMap=sorted O(log n); compareTo==0 means equal. Fail-fast=modCount bug detector (use `removeIf`). Immutable: `List.of`/`copyOf`. LinkedList rarely; ArrayDeque for deque.

## 04 Generics → [chapter](../04-generics-and-type-system/)
Generics=**compile-time** safety, no casts; **erased** at runtime (List<String>→List) → no new T[], T.class, instanceof List<String>, no type-param-only overloads; bridge methods. **Invariant** (List<String>≠List<Object>) vs arrays **covariant+reified** (ArrayStoreException). **PECS**: ? extends=producer(read), ? super=consumer(write, read as Object). Raw types→heap pollution/CCE—never use. Frameworks need TypeReference due to erasure.

## 05 Exceptions → [chapter](../05-exceptions-and-resource-management/)
Checked(compiler-forced/recoverable) vs unchecked(programming/unrecoverable); don't catch Error. Modern=prefer unchecked, wrap+preserve cause. **Spring rolls back unchecked only.** **try-with-resources**: AutoCloseable, reverse order, even on exception; close failure=**suppressed**. Never swallow. Errors→@ControllerAdvice→stable status+ProblemDetail, no stack traces, correlation id. No return/throw in finally. Exceptions=exceptional; Optional/result=expected absence.

## 06 Concurrency → [chapter](../06-java-concurrency/)
Three problems: **atomicity / visibility / ordering**. `volatile`=visibility+ordering only. `i++`=RMW→atomic/lock. Happens-before edges: program order, monitor unlock→lock, volatile write→read, start, join, final-field, transitive. `synchronized`=mutex+edge; `ReentrantLock` adds tryLock/timeout/fair/conditions (unlock in finally). Atomics=CAS lock-free; `LongAdder` for hot writes. Pools: grow past core **only when queue full**; bound the queue; rejection = Abort/CallerRuns/Discard/DiscardOldest; size = cores (CPU) or downstream capacity (IO). `CompletableFuture`: pass your executor, handle errors, don't block request thread. Virtual threads: IO-bound scale, no CPU gain, beware pinning (prefer ReentrantLock), one-per-task. Spring: singletons shared→stateless; tx/MDC/SecurityContext thread-bound.

## 07 JVM → [chapter](../07-jvm-memory-and-performance/)
Memory: **heap**(objects, young/old) + **stack**(frames/thread) + **Metaspace**(classes). Leak=**reachable-but-unneeded** (static maps, ThreadLocals, listeners). OOM(heap/metaspace) vs StackOverflow(recursion). GC: generational, G1 default; ZGC/Shenandoah low-pause. **Investigate leak:** GC logs confirm (old gen grows after full GC) → heap dump + MAT dominator tree → fix roots → verify. **CPU vs IO bound:** measure CPU + flame graph + thread dump. Benchmark with JMH + warm-up. Refs: soft(pressure)/weak(next GC)/phantom(cleanup).

## 08 Modern Java → [chapter](../08-java-8-plus-and-modern-java/)
Lambda=functional interface; captures effectively-final. Streams: **lazy intermediate** → **eager terminal**; single-use; short-circuit via findFirst/limit. **map(1→1), flatMap(flatten)**; **collect(build), reduce(fold)**. Optional=**return type**; use map/orElse/orElseThrow, never blind get()/field/param. **No side effects**; collect don't mutate. **Parallel streams = shared common pool** → avoid for IO/blocking, need associativity+thread-safety. Loops for index/early-exit/debug. java.time immutable; store UTC.

## 09 Spring Core DI → [chapter](../09-spring-core-and-dependency-injection/)
IoC+DI: container builds BeanDefinitions (scan + @Bean) → instantiate → inject by type (@Qualifier/@Primary) → BeanPostProcessors create **proxies** (@Transactional/@Async/@Cacheable) → @PostConstruct. **Constructor injection default** (final/explicit/testable/fail-fast on cycles). ApplicationContext⊃BeanFactory. Spring singleton=per-container→**stateless**. Self-invocation bypasses proxies. Cycle=design smell; @Lazy/extract/setter to break. Scopes: singleton/prototype/request/session.

## 10 Spring Boot → [chapter](../10-spring-boot-internals/)
Boot = starters + **conditional auto-config** + embedded server + externalized config/actuator. @SpringBootApplication=config+scan+enableAutoConfig. Auto-config @ConditionalOnClass/OnMissingBean/OnProperty → **backs off when you define your own**. BOM pins versions. **Precedence: CLI/env > profile files > application.yml > defaults.** @ConfigurationProperties=typed binding; secrets via env/secret manager. Debug beans: `debug=true` / /actuator/conditions. Secure actuator. Executable JAR default.

## 11 Spring MVC → [chapter](../11-spring-mvc-and-rest-apis/)
**Lifecycle:** container thread → filters(raw, all requests) → DispatcherServlet → HandlerMapping → interceptors → arg resolution + @Valid → controller → service(@Transactional) → repo/DB → HttpMessageConverter(JSON) → interceptors → filters → client. **One request=one thread.** Filters(raw/broad) vs interceptors(handler) vs AOP(bean methods). **DTOs/records at boundary, never entities.** @Valid(bodies)/@Validated(groups/params). Errors → @ControllerAdvice + ProblemDetail; 400/404/409/401/403/500. Stream big files.

## 12 JPA/Hibernate → [chapter](../12-spring-data-jpa-and-hibernate/)
States: transient/managed/detached/removed. Persistence context = per-tx 1st-level cache, mandatory. **Flush=emit SQL (not durable); commit=durable.** Managed entities dirty-checked → UPDATE without save(). `merge` returns managed copy. @ManyToOne EAGER, collections LAZY. **N+1 = 1 + N queries**; fix: fetch join (no collection pagination), entity graph, DTO projection, @BatchSize. Optimistic(@Version) vs pessimistic(SELECT FOR UPDATE). Disable OSIV; fetch in service. Batch writes + flush/clear. Entity equals/hashCode: business key, never all fields.

## 13 Spring Transactions → [chapter](../13-spring-transactions/)
**@Transactional = proxy + thread-bound tx.** Bypassed by self-invocation & non-public methods. Begin→commit(flush)/rollback→release. Rollback default: unchecked only; checked **commits** unless `rollbackFor`; swallowed exception commits → `setRollbackOnly()`. Propagation: REQUIRED(join), REQUIRES_NEW(independent, separate conn, deadlock risk), NESTED(savepoint). `readOnly`=hint. Tx ≠ cross-thread: @Async has no tx → pass ids, own @Transactional, afterCommit. Long tx → pool exhaustion. Cross-service: outbox/saga over XA.

## 14 Spring Security → [chapter](../14-spring-security/)
Authn(identity)+authz(permissions) in the **filter chain** → Authentication in SecurityContext (ThreadLocal). Session(easy revoke, stateful) vs **JWT(stateless, hard revoke)** → short access + refresh + denylist. **Always verify JWT signature + exp/iss/aud; reject alg confusion; payload is base64 not encrypted.** Passwords: **BCrypt/Argon2 + salt**. CORS(origin policy)≠CSRF(cookie attack); disable CSRF only for cookieless Bearer APIs. Least-privilege authorities; @PreAuthorize.

## 15 SQL → [chapter](../15-sql-and-relational-databases/)
Declarative + constraints. **LEFT join: right-col predicate in ON, not WHERE.** WHERE(pre-group) vs HAVING(post-agg). **NULL=unknown**: IS NULL, NOT EXISTS (not NOT IN). EXISTS short-circuits/NULL-safe. UNION ALL default. **Window funcs** = per-row over partition: top-N, running total, dedup. **Keyset > OFFSET** deep paging. Upsert ON CONFLICT(PG)/MERGE(Oracle). Oracle: ''=NULL, no boolean, OFFSET…FETCH.

## 16 Indexing → [chapter](../16-database-indexing-and-query-optimization/)
B-tree=sorted → equality/range/prefix 'x%'/ORDER BY; NOT '%x' or function-on-column. **Composite (a,b,c) = left-prefix only; equality cols first, range last.** Covering/INCLUDE → index-only scan. **Sargable** = compare bare column; fix with expression index/normalized column. Low selectivity → ignored. Stale stats → bad plans (ANALYZE/DBMS_STATS); spot via estimated≠actual rows. Indexes slow writes. **Always EXPLAIN ANALYZE.**

## 17 DB Transactions → [chapter](../17-database-transactions-and-isolation/)
**ACID**; Consistency=app+constraints. Anomalies: dirty/non-repeatable/phantom + **lost update/write skew**. ANSI levels=floor. **PG & Oracle default READ COMMITTED, MVCC.** PG RR=snapshot (no phantoms) but write skew possible; SERIALIZABLE aborts→retry. Lost update: `col=col+x` / FOR UPDATE / `@Version` / SERIALIZABLE. Write skew → SERIALIZABLE or explicit lock. Dup on retry → **unique constraint** + idempotency key + upsert. FOR UPDATE SKIP LOCKED = queue workers. Long tx → pool exhaustion + bloat/undo. Deadlock → consistent order + retry.

## 18 API Engineering → [chapter](../18-api-engineering/)
Safe(no change) vs idempotent(repeat-safe); **POST neither → idempotency key (mechanism) + unique constraint**. Status: 400/401/403/404/409/422/429/5xx; ProblemDetail + correlation id. **Timeout everywhere.** Retries = exponential **backoff + jitter + budget + circuit breaker**, idempotent only. Keyset pagination. Version compatibly. Bulk: all-or-nothing vs per-item. ETag/If-None-Match→304. Rate limit 429+Retry-After.

## 19 Messaging → [chapter](../19-messaging-and-asynchronous-processing/)
Async decouples; queues=durable + load leveling (executor=not durable). **At-least-once default → duplicates → idempotent consumers** (dedup by message id + unique constraint, same tx). Exactly-once = effectively-once *processing* via dedup. Poison msg → DLQ after N. Kafka orders **within partition**; key by ordering entity. Bounded prefetch = backpressure. **Outbox** = business row + outbox row in one tx, relay publishes (fixes dual-write). @Scheduled runs on every instance → distributed lock.

## 20 Caching → [chapter](../20-caching-and-performance/)
Cache = copy → staleness + invalidation decision. **Local(fast, per-node divergent) vs distributed(shared, network).** Cache-aside(default)/write-through(fresh)/write-behind(fast, risky). **Always bound size+TTL** (else leak). **Invalidate DB-then-cache, evict after commit**; don't cache uncommitted. Stampede → single-flight + TTL jitter + refresh-ahead. Negative-cache missing keys. Caching hurts: low hit rate, big objects, remote hop, no fallback. Correctness > speed.

## 21 Testing → [chapter](../21-testing-and-code-quality/)
**Pyramid**: many unit / fewer integration / thin E2E. Doubles: stub(state)/mock(interaction)/spy. **Over-mocking hides bugs**; test real DB. **@WebMvcTest**(web slice) vs **@SpringBootTest**(full). **Testcontainers** = real DB (H2 hides dialects). **@Transactional tests roll back → hide commit-time bugs.** Async: Awaitility, not sleep. Flaky = bug. Coverage ≠ assertions. Characterization tests before legacy refactor.

## 22 Debugging → [chapter](../22-debugging-and-production-engineering/)
**Process: symptoms→hypotheses→evidence→root cause→fix→verify→prevent.** Tools: trace(latency), flame graph(CPU), heap dump+MAT(memory), jstack(hang/deadlock), EXPLAIN ANALYZE(query), pool metrics+pg_stat_activity(pool). Common roots: N+1(→fetch join), leak(→bound), pool exhaustion(→short tx), deadlock(→order+retry), duplicates(→idempotency+unique+afterCommit), no-rollback(→checked/swallowed/self-call), retry storm(→backoff+jitter+breaker), saturation(→bounded queue). "Works locally"=env/data/concurrency diff.

## 23 Networking/HTTP → [chapter](../23-networking-and-http-fundamentals/)
Call = **DNS → TCP(3-way) → TLS(1.3 ~1 RTT, validates cert) → HTTP**. First HTTPS request slow → **reuse pooled keep-alive connections**. HTTP/1.1(one-at-a-time, HOL) → HTTP/2(multiplex) → HTTP/3(QUIC/UDP, no TCP HOL). **Always set connect + read timeouts.** Pool: size to concurrency, recycle idle. Reverse proxy/LB has own timeouts (502/504). **CORS = browser policy, not server auth** (preflight OPTIONS).

## 24 Build/Deploy → [chapter](../24-build-tools-and-deployment-fundamentals/)
**One immutable artifact + external config** across envs. Maven: **nearest-wins** conflict → `dependency:tree` + BOM. **Executable fat JAR** default. Docker: **multi-stage** (JDK build/JRE run), layer caching, **non-root**, cgroup-aware JVM, no secrets in image. **Graceful shutdown** (drain in-flight) + readiness gating. Release: canary/rolling + backward-compatible migrations + rollback. CI/CD: build→test→scan→package→deploy→promote.

## 25 Backend Security → [chapter](../25-backend-security/)
**Don't trust input**: validate/allowlist server-side. **Parameterized queries always** (never concat → injection). Secrets: env/secret manager, never in code/images/logs. Authz **server-side + object-level** (prevent IDOR). Passwords BCrypt/Argon2; TLS; encrypt at rest; mask PII. Avoid native deserialization (RCE); guard SSRF. Patch/scan deps. **Generic errors to clients**, details+correlation id server-side. Rate-limit auth; MFA. Secure defaults.

## 26 Observability/Reliability → [chapter](../26-observability-and-reliability/)
**Metrics(detect/alert) → traces(localize) → logs(explain).** p95/p99 not averages. Golden signals: latency/traffic/errors/saturation. SLI/SLO/**error budget**(1−SLO) → alert on user impact. Structured logs + **correlation id**. **Timeouts everywhere.** **Circuit breaker**: open→half-open→closed. **Bulkhead** isolates per-dep pools. **Liveness**(restart) vs **readiness**(remove from LB). Graceful degradation + shutdown.

## 27 Cross-topic → [chapter](../27-cross-topic-interview-scenarios/)
12 scenarios: clarify first, reason from evidence, give the trade-off. Doc revisions→@Version; slow endpoint/fast query→N+1; dup notifications→idempotent+afterCommit+outbox; pool exhaustion→short tx/OSIV off; silent persist→checked/swallowed/self-call; queued tasks→bounded queue; fast-in-dev→data/index/stats; stale cache→after-commit evict; timeout+retry dup→unique constraint; cron twice→distributed lock; partial import→idempotent chunks; low CPU high latency→waiting (thread dump+trace).

## 28 Mock Interviews → [chapter](../28-mock-interviews/)
8 rounds + practice mode (questions then key). Ready when you consistently clarify, state guarantees precisely, survive 2 follow-ups, give trade-offs with a pick, and name the evidence you'd gather.
