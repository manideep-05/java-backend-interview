# Interview Question Index

Centralized, de-duplicated question bank. Each question has an ID, topic, difficulty/priority, what's being tested, expected answer points, and a pointer to the chapter holding the full sample answer, follow-ups, traps, and code/SQL. This index tests **understanding and application**, not definitions.

**Priority:** `P0` must master · `P1` important · `P2` advanced / lower-frequency.
Priority reflects conceptual importance and backend relevance — **not** a claim that any specific company asks a given question.

**ID scheme:** `CJ` core Java · `COL` collections · `GEN` generics · `EXC` exceptions · `MJ` modern Java · `CON` concurrency · `JVM` jvm · `SC` spring core · `SB` spring boot · `MVC` spring mvc · `JPA` jpa/hibernate · `TX` spring tx · `SEC` Spring Security · `SEC25` backend security (ch.25, prefixed to avoid collision) · `SQL` sql · `IDX` indexing · `DBTX` db transactions · `API` api · `MSG` messaging · `CACHE` caching · `TEST` testing · `DBG` debugging · `NET` networking · `BLD` build/deploy · `OBS` observability · `XT` cross-topic · `BEH` behavioral/STAR (ch.29) · `NODE` Node.js (ch.30, conditional) · `DSA` data-structures & algorithms patterns (ch.31; problems grouped by pattern in `31-dsa-patterns/question-bank.md`).

Full answers live in each chapter's section **E** (ch.22 uses §C playbooks + §E; ch.27/28 are self-contained). **All 28 chapters are complete**, so every ID below is answered in its chapter — see `PROGRESS.md` for build status.

---

## P0 — must master

| ID | Topic | What's tested | Chapter |
|---|---|---|---|
| CJ-01 | `==` vs `equals`, Integer cache | reference vs value equality, autoboxing trap | 01 §E |
| CJ-04 | equals/hashCode contract | map/set correctness implications | 01 §E |
| CJ-07 | Why String is immutable | security, pooling, hashing, thread-safety | 01 §E |
| CJ-09 | Mutable object as HashMap key | why entries get "lost" | 01 §E |
| COL-01 | Why HashMap isn't thread-safe | lost updates, resize corruption, infinite loop (pre-8) | 03 §E |
| COL-02 | How HashMap handles collisions | chaining → treeify at threshold | 03 §E |
| COL-03 | Why ConcurrentHashMap differs; not a universal replacement | per-bin locking/CAS; compound ops still race | 03 §E |
| COL-06 | Fail-fast iterators & CME | modCount, when it does/doesn't throw | 03 §E |
| CON-01 | Why volatile is insufficient for `i++` | atomicity vs visibility | 06 §E |
| CON-02 | happens-before / JMM | visibility & ordering guarantees | 06 §E |
| CON-04 | Why a bigger thread pool can be slower | contention, context switches, queueing | 06 §E |
| CON-05 | Thread-pool saturation & rejection | queue choice, RejectedExecutionHandler | 06 §E |
| CON-07 | Deadlock: cause, detection, prevention | lock ordering, tryLock | 06 §E |
| CON-09 | Virtual threads: when they help / don't | IO-bound vs CPU-bound, pinning | 06 §E |
| JVM-02 | Investigate a production memory leak | heap dump, dominator tree, retained refs | 07 §E |
| JVM-05 | CPU-bound vs IO-bound bottleneck | how to tell them apart with evidence | 07 §E |
| SC-01 | How Spring DI works internally | context, bean definitions, injection | 09 §E |
| SC-04 | Constructor vs field vs setter injection | testability, immutability, circular deps | 09 §E |
| MVC-03 | Filter vs interceptor vs AOP | where each sits in the request path | 11 §E |
| MVC-05 | DTO vs entity at the boundary | leaking persistence model, over-posting | 11 §E |
| JPA-03 | flush vs commit | SQL emission vs transaction durability | 12 §E |
| JPA-05 | How Hibernate produces N+1 | lazy associations in a loop | 12 §E |
| JPA-08 | Optimistic vs pessimistic locking | contention profile, retries | 12 §E |
| TX-02 | Why `@Transactional` fails on self-invocation | proxy bypass | 13 §E |
| TX-04 | Rollback rules & checked exceptions | default rollback only on unchecked | 13 §E |
| TX-06 | Why a transaction doesn't cross threads | thread-bound EntityManager/connection | 13 §E |
| DBTX-02 | Isolation levels & anomalies | dirty/non-repeatable/phantom mapping | 17 §E |
| DBTX-05 | Prevent a lost update | SELECT…FOR UPDATE vs @Version | 17 §E |
| DBTX-07 | Deadlock investigation in the DB | lock ordering, logs, retry | 17 §E |
| DBTX-09 | Client times out after DB commit | idempotency, duplicate records | 17 §E / 27 |
| SQL-04 | EXISTS vs IN (and NULL pitfalls) | semantics & performance | 15 §E |
| SQL-07 | Window function problem | ranking/running totals | 15 §E |
| IDX-02 | Why a correct-looking composite index is ignored | column order, leading predicate, selectivity | 16 §E |
| IDX-05 | Sargability | functions on columns, leading wildcard | 16 §E |
| API-03 | Idempotent HTTP method ≠ idempotency key | semantics vs dedup mechanism | 18 §E |
| API-06 | Why retries can worsen an outage | retry storms, budgets, backoff+jitter | 18 §E |
| DBG-01..12 | Production incident playbooks | disciplined investigation | 22 §C |
| XT-01..12 | Cross-topic scenarios | connecting multiple concepts | 27 |

## P1 — important

| ID | Topic | What's tested | Chapter |
|---|---|---|---|
| CJ-05 | Immutability & defensive copying | safe sharing, builder patterns | 01 §E |
| CJ-11 | String vs StringBuilder vs StringBuffer | concatenation cost, thread-safety | 01 §E |
| COL-04 | ArrayList vs LinkedList | access vs structural modification cost | 03 §E |
| COL-05 | Comparable vs Comparator | natural vs external ordering | 03 §E |
| GEN-02 | PECS / bounded wildcards | producer-extends, consumer-super | 04 §E |
| GEN-03 | Type erasure consequences | no reified generics, bridge methods | 04 §E |
| EXC-02 | try-with-resources & suppressed exceptions | deterministic close ordering | 05 §E |
| EXC-04 | Checked vs unchecked design | when each is appropriate | 05 §E |
| MJ-03 | map vs flatMap | nested stream flattening | 08 §E |
| MJ-05 | Optional misuse | fields/params vs return types | 08 §E |
| MJ-07 | Parallel streams risks | common pool, non-associative ops | 08 §E |
| CON-11 | CompletableFuture exception handling | handle/exceptionally, composition | 06 §E |
| JVM-03 | OOM vs StackOverflow | heap vs stack exhaustion | 07 §E |
| SB-02 | Which auto-config created a bean | conditions report, debugging | 10 §E |
| SB-03 | Config precedence | env/profile/property ordering | 10 §E |
| SEC-02 | Common JWT mistakes | expiry, alg confusion, storage | 14 §E (see also 25 §E) |
| TX-03 | Propagation modes | REQUIRED vs REQUIRES_NEW vs NESTED | 13 §E |
| JPA-06 | Fetch join / entity graph / projection | fixing N+1 the right way | 12 §E |
| MSG-03 | At-least-once + idempotent consumer | dedup, outbox | 19 §E |
| CACHE-04 | Caching causing correctness bugs | stale data, invalidation ordering | 20 §E |
| CACHE-07 | Redis vs Caffeine; Redis internals | single-thread, structures, persistence, consistency | 20 §E |
| NODE-01 | Node event loop & task ordering | microtasks vs macrotasks | 30 §E |
| NODE-02 | Blocking the event loop | CPU work on single thread | 30 §E |
| NODE-06 | Streams & backpressure | write()→drain, pipeline | 30 §E |
| BEH-ALL | Behavioral / STAR stories | ownership, trade-offs, failure, conflict, mentoring | 29 |
| DSA-ALL | DSA patterns + ~110 curated problems | pattern recognition, Java, complexity, follow-ups | 31 (see `31-dsa-patterns/`) |
| TEST-03 | `@WebMvcTest` vs `@SpringBootTest` | slice vs full context | 21 §E |
| NET-02 | TLS handshake basics | what happens on HTTPS connect | 23 §E |
| OBS-02 | Metrics vs logs vs traces | when to reach for each | 26 §E |

## P2 — advanced / lower-frequency

| ID | Topic | What's tested | Chapter |
|---|---|---|---|
| CON-13 | Phaser / CyclicBarrier nuances | reusable barriers | 06 §E |
| JVM-07 | Phantom/soft/weak references | cache/cleanup semantics | 07 §E |
| NET-04 | HTTP/2 vs HTTP/3 | multiplexing, QUIC | 23 §E |
| BLD-02 | JAR vs WAR, Maven resolution | packaging & dependency mediation | 24 §E |

---

## Representative seed questions (expanded in chapters)

These are explicitly requested, high-signal prompts; full treatment in the referenced chapter:

- Why is HashMap not thread-safe? → **COL-01**
- How does HashMap handle collisions? → **COL-02**
- Why does ConcurrentHashMap behave differently? → **COL-03**
- Why is volatile insufficient for incrementing a counter? → **CON-01**
- How does Spring dependency injection work internally? → **SC-01**
- Why does `@Transactional` fail during self-invocation? → **TX-02**
- What is the difference between flush and commit? → **JPA-03**
- How does Hibernate produce the N+1 problem? → **JPA-05**
- Why can a correct-looking composite index be ignored? → **IDX-02**
- How can two requests create duplicate records? → **DBTX-09 / XT-09**
- Why can increasing a thread pool make performance worse? → **CON-04**
- How do you distinguish an application bottleneck from a database bottleneck? → **JVM-05 / DBG**
- How would you investigate a production memory leak? → **JVM-02**
- How can an asynchronous job send duplicate emails? → **XT-03 / MSG-03**
- What happens if a client times out after the database commits? → **DBTX-09**
- How do you prevent lost updates? → **DBTX-05**
- Why is an idempotent HTTP method not the same as an idempotency key? → **API-03**
- When should an application use optimistic versus pessimistic locking? → **JPA-08**
- Why might caching introduce correctness bugs? → **CACHE-04**
- Why does adding retries sometimes make an outage worse? → **API-06**
