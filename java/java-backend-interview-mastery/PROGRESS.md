# Progress Tracker — single source of truth

Two things live here:
1. **Repository build status** — which chapters this repo has actually populated (for whoever is generating/extending it).
2. **Your learning progress** — your personal mastery status per topic.

---

## Status legend

**Repository build status:**
- `COMPLETE` — full template coverage, code present, question bank present.
- `PARTIAL` — started; core sections present but not the full template.
- `PENDING` — directory exists, content not yet written.

**Your learning status:**
- `Not Started`
- `Learning` — reading/understanding.
- `Practicing` — implementing and drilling questions.
- `Needs Revision` — understood once, slipping; schedule a revisit.
- `Mastered` — meets all five criteria in README ("Definition of Mastered").

---

## 1. Repository build status

| # | Chapter | Build status | Notes |
|---|---|---|---|
| 01 | core-java | COMPLETE | Full chapter: object semantics, equals/hashCode, immutability, String pool, records/sealed. CJ-01..CJ-12. |
| 02 | oop-and-java-design-fundamentals | COMPLETE | Polymorphism/binding, composition vs inheritance, interface vs abstract. OOP-01..OOP-06. |
| 03 | collections-framework | COMPLETE | HashMap internals, ConcurrentHashMap, fail-fast, Comparable/Comparator. COL-01..COL-08. |
| 04 | generics-and-type-system | COMPLETE | PECS, type erasure, invariance, heap pollution. GEN-01..GEN-05. |
| 05 | exceptions-and-resource-management | COMPLETE | Checked/unchecked, try-with-resources, suppressed, error design. EXC-01..EXC-06. |
| 06 | java-concurrency | COMPLETE | JMM, locks, volatile, atomics, pools, CompletableFuture, virtual threads. CON-01..CON-13. |
| 07 | jvm-memory-and-performance | COMPLETE | GC, leaks, heap/thread dumps, CPU vs IO, benchmarking. JVM-01..JVM-07. |
| 08 | java-8-plus-and-modern-java | COMPLETE | Streams, Optional, map/flatMap, parallel stream risks, java.time. MJ-01..MJ-08. |
| 09 | spring-core-and-dependency-injection | COMPLETE | IoC/DI, bean lifecycle, proxies, circular deps, scopes. SC-01..SC-07. |
| 10 | spring-boot-internals | COMPLETE | Auto-config conditions, starters/BOM, config precedence, actuator. SB-01..SB-05. |
| 11 | spring-mvc-and-rest-apis | COMPLETE | Request lifecycle, filter/interceptor/AOP, DTO vs entity, validation, errors. MVC-01..MVC-07. |
| 12 | spring-data-jpa-and-hibernate | COMPLETE | Persistence context, N+1, flush/commit, locking. JPA-01..JPA-10. |
| 13 | spring-transactions | COMPLETE | @Transactional internals, propagation, self-invocation, async boundary. TX-01..TX-08. |
| 14 | spring-security | COMPLETE | Filter chain, session vs JWT, JWT mistakes, CORS vs CSRF, password hashing. SEC-01..SEC-06. |
| 15 | sql-and-relational-databases | COMPLETE | Joins, window funcs, NULL/3VL, EXISTS vs IN, keyset pagination, PG vs Oracle. SQL-01..SQL-09. |
| 16 | database-indexing-and-query-optimization | COMPLETE | B-tree, composite column order, covering, sargability, EXPLAIN. IDX-01..IDX-07. |
| 17 | database-transactions-and-isolation | COMPLETE | ACID, anomalies, MVCC, locking, deadlocks, idempotency. DBTX-01..DBTX-10. |
| 18 | api-engineering | COMPLETE | Idempotency key vs method, retries/backoff/jitter/breaker, versioning, bulk. API-01..API-07. |
| 19 | messaging-and-asynchronous-processing | COMPLETE | Delivery guarantees, idempotent consumers, DLQ, outbox, ordering. MSG-01..MSG-06. |
| 20 | caching-and-performance | COMPLETE | Cache-aside, invalidation ordering, stampede, correctness, when caching hurts. CACHE-01..CACHE-06. |
| 21 | testing-and-code-quality | COMPLETE | Pyramid, doubles, slices, Testcontainers, tx-test caveat, async/flaky. TEST-01..TEST-06. |
| 22 | debugging-and-production-engineering | COMPLETE | General workflow + 12 incident playbooks DBG-01..DBG-12 + DBG-A/B/C. |
| 23 | networking-and-http-fundamentals | COMPLETE | DNS/TCP/TLS, HTTP/1.1/2/3, keep-alive, timeouts, CORS. NET-01..NET-06. |
| 24 | build-tools-and-deployment-fundamentals | COMPLETE | Maven resolution, JAR vs WAR, Docker, graceful shutdown, CI/CD. BLD-01..BLD-06. |
| 25 | backend-security | COMPLETE | Injection, secrets, IDOR, deserialization/SSRF, info leakage. SEC25-01..SEC25-06. |
| 26 | observability-and-reliability | COMPLETE | Metrics/logs/traces, percentiles, SLOs, circuit breaker, bulkhead, probes. OBS-01..OBS-06. |
| 27 | cross-topic-interview-scenarios | COMPLETE | 12 full scenarios XT-01..XT-12 (clarify/investigate/root cause/fix/trade-offs/polished answer). |
| 28 | mock-interviews | COMPLETE | 8 rounds + rubric + practice mode (20 questions) + answer key. |
| 29 | behavioral-and-leadership | COMPLETE | STAR framework + 7 resume-seeded stories (B1–B7) + pitfalls + prep checklist. |
| 30 | nodejs-backend-fundamentals | COMPLETE | Event loop, async/await, worker_threads/cluster, streams/backpressure, Node vs JVM. NODE-01..NODE-07. |
| 31 | dsa-patterns | COMPLETE | 21 pattern files + master README (identification table) + question-bank.md (~110 problems) + cheatsheet.md. Java solutions, complexity, follow-ups. |
| — | code-examples (Maven project) | COMPLETE | Java 21 Maven project; CounterDemo/DocumentPublisher/Period + 8 JUnit 5 tests. **Verified: `mvn test` BUILD SUCCESS, 8/8 pass.** |
| — | sql-examples | COMPLETE | 4 scripts: schema+seed, joins/windows, indexing/plans, isolation/lost-update. PG + Oracle notes. |
| — | revision | COMPLETE | README + tier0-cheatsheet.md + all-one-pagers.md (all 28 chapter sheets). |

**Batch log**
- Batch 1: skeleton + README/ROADMAP/PROGRESS/INDEX/CHECKLIST; Tier-0 chapters 06, 03, 13, 12, 17, 01 (deep full-template).
- Batch 2: chapters 09, 11 (P0 Spring) + 07, 08, 05, 04, 02, 10, 14 (proportionate full-template). All Java + Spring chapters (01–14) now COMPLETE.
- Batch 3: chapters 15, 16, 22 (Tier 0) + 18, 19, 20, 21, 23, 24, 25, 26. All topic chapters (01–26) COMPLETE.
- Batch 4: chapter 27 (cross-topic XT-01..XT-12), chapter 28 (mock interviews + practice mode), code-examples (Java 21 Maven project — **verified `mvn test` BUILD SUCCESS, 8/8 tests pass**), sql-examples (4 scripts), revision (cheatsheets). **Repository COMPLETE — all 28 chapters + supporting dirs populated.**

- Batch 5 (gap-closing after loop review): chapter 29 (Behavioral/STAR, resume-seeded), chapter 30 (Node.js fundamentals, conditional), Redis depth added to chapter 20 (CACHE-07/08).
- Batch 6: chapter 31 (DSA patterns) — 21 pattern files under 31-dsa-patterns/patterns/, master README with identification table, question-bank.md (~110 curated problems grouped by pattern), cheatsheet.md. DSA is now IN-repo (earlier versions excluded it).

**Status: COMPLETE (31 chapters).** All chapters, question banks, runnable+verified code, SQL scripts, and revision material are in place. **Not covered here (by design — separate prep):** LLD design problems and HLD system-design case studies. Future work would be optional expansion (more code examples, Testcontainers tests, runnable DSA solutions module), not missing core content.

---

## 2. Your learning progress

Fill the Status column as you go. Date your last review to drive spaced repetition.

| Topic | Priority | Status | Last reviewed | Next checkpoint |
|---|---|---|---|---|
| Core Java object semantics | P0 | Not Started | | R1 |
| equals/hashCode contract | P0 | Not Started | | R1 |
| Immutability & defensive copying | P0 | Not Started | | R1 |
| Collections — HashMap internals | P0 | Not Started | | R2 |
| ConcurrentHashMap | P0 | Not Started | | R2/R3 |
| Generics & PECS | P1 | Not Started | | R2 |
| Exceptions & try-with-resources | P1 | Not Started | | R2 |
| Streams / Optional / CompletableFuture | P1 | Not Started | | R2 |
| Concurrency — JMM & happens-before | P0 | Not Started | | R3 |
| volatile vs atomics vs locks | P0 | Not Started | | R3 |
| Thread pools & saturation | P0 | Not Started | | R3 |
| Virtual threads | P1 | Not Started | | R4 |
| JVM GC & memory leaks | P0 | Not Started | | R4 |
| Spring Core DI & proxies | P0 | Not Started | | R5 |
| Spring Boot internals | P1 | Not Started | | R5 |
| Spring MVC request lifecycle | P0 | Not Started | | R5 |
| JPA persistence context & N+1 | P0 | Not Started | | R6 |
| @Transactional internals | P0 | Not Started | | R6 |
| SQL joins/windows/NULL | P0 | Not Started | | R7 |
| Indexing & execution plans | P0 | Not Started | | R7 |
| DB isolation & lost update | P0 | Not Started | | R7 |
| API idempotency & retries | P1 | Not Started | | R8 |
| Spring Security / JWT | P1 | Not Started | | R8 |
| Messaging & idempotent consumers | P1 | Not Started | | R8 |
| Caching correctness | P1 | Not Started | | R8 |
| Testing strategy | P1 | Not Started | | R9 |
| Networking / HTTP / TLS | P1 | Not Started | | R9 |
| Production debugging playbooks | P0 | Not Started | | R9 |
| Observability & reliability | P1 | Not Started | | R9 |
| Cross-topic scenarios | P0 | Not Started | | R10 |
| Mock interviews | P0 | Not Started | | Final |

> **Mastered means all five:** explain without notes · implement · diagnose a failure · compare alternatives · survive two layers of follow-ups.
