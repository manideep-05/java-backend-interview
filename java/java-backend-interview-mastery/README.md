# Java Backend Interview Mastery

A structured, interview-oriented reference for engineers preparing for **SDE-2 / backend engineer** roles at product companies. Focused on **Java, Spring Boot, and SQL** backend engineering fundamentals.

> **Scope note.** Core focus is the Java / database / concurrency / API / production-engineering fundamentals that SDE-2 interviewers probe for depth (chapters 01–26), plus behavioral (ch.29), optional Node.js (ch.30), and a **pattern-based DSA module (ch.31)**. **LLD design problems (Parking Lot, BookMyShow, etc.) and HLD case studies (design YouTube/WhatsApp/Uber) are still out of scope** — prepare those separately. DSA, which earlier versions excluded, is now covered as patterns in chapter 31.

---

## How to use this repository

1. **Learning path:** follow [`ROADMAP.md`](./ROADMAP.md) phase by phase.
2. **Track yourself:** update [`PROGRESS.md`](./PROGRESS.md) as you move each topic from *Not Started* to *Mastered*.
3. **Drill questions:** [`INTERVIEW_QUESTION_INDEX.md`](./INTERVIEW_QUESTION_INDEX.md) is the centralized question bank, tagged P0/P1/P2.
4. **Final prep:** [`REVISION_CHECKLIST.md`](./REVISION_CHECKLIST.md) is the day-before-interview sweep.
5. **Run code:** `code-examples/` holds a runnable Maven project (Java 21). `sql-examples/` holds portable SQL scripts with dialect notes.

Each chapter follows a consistent template (concept → internals → code → trade-offs → interview Q&A → production relevance → revision). See [Chapter template](#chapter-template) below.

---

## Technology baseline

| Thing | Version used in examples | Notes |
|---|---|---|
| Java | **21 (LTS)** | Virtual threads, records, sealed classes, pattern matching available. Version-specific behavior is flagged inline. |
| Spring Boot | **3.3.x** | Requires Java 17+; built and tested examples target Java 21. Spring Framework 6.x underneath. |
| Build tool | **Maven** | Used unless a case specifically justifies otherwise. |
| Databases | **PostgreSQL 16** and **Oracle 19c** | SQL examples flag the target dialect whenever syntax or semantics differ. |
| Testing | **JUnit 5**, **Mockito**, **Testcontainers** | |

When behavior depends on version, configuration, or runtime, the text says so explicitly and distinguishes **language guarantee** vs **framework behavior** vs **implementation detail** vs **configuration-dependent assumption**.

---

## Repository map

| # | Chapter | Priority | Why it matters for SDE-2 |
|---|---|---|---|
| 01 | [Core Java](./01-core-java/) | **P0** | Object semantics, equals/hashCode, immutability, String pool — baseline for everything. |
| 02 | [OOP & Java design fundamentals](./02-oop-and-java-design-fundamentals/) | P1 | Polymorphism, composition vs inheritance, interface design. |
| 03 | [Collections framework](./03-collections-framework/) | **P0** | HashMap internals, ConcurrentHashMap, choosing the right collection. |
| 04 | [Generics & type system](./04-generics-and-type-system/) | P1 | PECS, type erasure, invariance. |
| 05 | [Exceptions & resource management](./05-exceptions-and-resource-management/) | P1 | Checked vs unchecked, try-with-resources, error-response design. |
| 06 | [Java concurrency](./06-java-concurrency/) | **P0** | Highest-weight SDE-2 topic. JMM, locks, thread pools, CompletableFuture, virtual threads. |
| 07 | [JVM memory & performance](./07-jvm-memory-and-performance/) | **P0** | GC, leaks, heap/thread dumps, evidence-based tuning. |
| 08 | [Java 8+ & modern Java](./08-java-8-plus-and-modern-java/) | P1 | Streams, Optional, CompletableFuture, records/sealed. |
| 09 | [Spring core & DI](./09-spring-core-and-dependency-injection/) | **P0** | IoC, bean lifecycle, proxies, circular deps. |
| 10 | [Spring Boot internals](./10-spring-boot-internals/) | P1 | Auto-configuration, starters, externalized config, actuator. |
| 11 | [Spring MVC & REST APIs](./11-spring-mvc-and-rest-apis/) | **P0** | Request lifecycle, DTOs vs entities, validation, @ControllerAdvice. |
| 12 | [Spring Data JPA & Hibernate](./12-spring-data-jpa-and-hibernate/) | **P0** | Persistence context, N+1, flush vs commit, locking. |
| 13 | [Spring transactions](./13-spring-transactions/) | **P0** | @Transactional internals, propagation, self-invocation, async boundary. |
| 14 | [Spring Security](./14-spring-security/) | P1 | Filter chain, JWT vs sessions, method security. |
| 15 | [SQL & relational databases](./15-sql-and-relational-databases/) | **P0** | Joins, window functions, NULL semantics, keyset pagination. |
| 16 | [Indexing & query optimization](./16-database-indexing-and-query-optimization/) | **P0** | B-tree indexes, composite column order, execution plans, sargability. |
| 17 | [DB transactions & isolation](./17-database-transactions-and-isolation/) | **P0** | ACID, isolation anomalies, MVCC, lost update, deadlocks, idempotency. |
| 18 | [API engineering](./18-api-engineering/) | P1 | Idempotency keys, retries/backoff, versioning, partial failures. |
| 19 | [Messaging & async processing](./19-messaging-and-asynchronous-processing/) | P1 | Delivery guarantees, idempotent consumers, outbox, DLQ. |
| 20 | [Caching & performance](./20-caching-and-performance/) | P1 | Cache-aside, invalidation, stampede, correctness over speed. |
| 21 | [Testing & code quality](./21-testing-and-code-quality/) | P1 | JUnit 5, Mockito, Testcontainers, flaky tests. |
| 22 | [Debugging & production engineering](./22-debugging-and-production-engineering/) | **P0** | Disciplined investigation workflows for real incidents. |
| 23 | [Networking & HTTP fundamentals](./23-networking-and-http-fundamentals/) | P1 | DNS, TCP, TLS, HTTP/2, keep-alive, timeouts. |
| 24 | [Build tools & deployment](./24-build-tools-and-deployment-fundamentals/) | P2 | Maven resolution, JAR vs WAR, Docker, graceful shutdown. |
| 25 | [Backend security](./25-backend-security/) | P1 | OWASP-relevant: injection, authn/authz, secrets. |
| 26 | [Observability & reliability](./26-observability-and-reliability/) | P1 | Metrics, logs, traces, SLOs, timeouts, circuit breakers. |
| 27 | [Cross-topic scenarios](./27-cross-topic-interview-scenarios/) | **P0** | Multi-concept production puzzles. |
| 28 | [Mock interviews](./28-mock-interviews/) | **P0** | Full rounds + practice mode (questions, then answer key). |
| 29 | [Behavioral & leadership (STAR)](./29-behavioral-and-leadership/) | **P0** | The behavioral round — STAR framework + pre-built stories. |
| 30 | [Node.js backend fundamentals](./30-nodejs-backend-fundamentals/) | P1 (conditional) | Event loop, async model, backpressure — if interviewed on Node. |
| 31 | [DSA patterns](./31-dsa-patterns/) | **P0** | 21 patterns, pattern identification, Java solutions, complexity, follow-ups + question bank. |

Supporting: [`code-examples/`](./code-examples/) · [`sql-examples/`](./sql-examples/) · [`revision/`](./revision/)

> **Scope reminder:** chapters 01–26 are the backend knowledge round; 27–28 are cross-topic + mock practice; **29 (behavioral)**, **30 (Node.js, conditional)**, and **31 (DSA patterns)** cover the rest of a full SDE-2 loop. **LLD and HLD remain separate prep** — this repo covers everything else (backend knowledge, behavioral, DSA) but not the design rounds.

---

## Topic ranking by SDE-2 interview importance

Ranking reflects **conceptual importance and practical backend relevance**, not any claim about specific companies' question histories.

**Tier 0 — master before interviewing (highest frequency + depth probed):**
1. Java concurrency (06)
2. Spring transactions (13)
3. Spring Data JPA / Hibernate — persistence context & N+1 (12)
4. Collections — HashMap/ConcurrentHashMap internals (03)
5. Database transactions & isolation (17)
6. SQL + indexing & query optimization (15, 16)
7. Core Java object semantics — equals/hashCode, immutability (01)
8. Production debugging workflows (22)
9. Spring core DI & proxies (09)
10. REST API lifecycle & correctness (11, 18)

**Tier 1 — important, commonly asked:**
JVM/GC & leaks (07), modern Java/streams (08), exceptions (05), generics (04), Spring Boot internals (10), Spring Security (14), caching (20), messaging (19), testing (21), networking/HTTP (23), backend security (25), observability (26), OOP design (02).

**Tier 2 — advanced or lower-frequency (support deeper answers):**
Build/deploy details (24), phantom-reference depth, Phaser, HTTP/3 internals.

---

## Chapter template

Every major concept is developed as:

- **A. Concept & intuition** — plain definition, analogy, the problem it solves, what breaks without it.
- **B. Internal working** — step-by-step execution, runtime/framework/DB behavior, assumptions & version notes.
- **C. Code demonstration** — minimal example, realistic backend example, a *broken* version, why it breaks, the fix, observable behavior.
- **D. Trade-offs** — comparison table (option / pros / cons / performance / complexity / when to use / when it's a poor choice).
- **E. Interview Q&A** — basic → SDE-2 depth → tricky → scenario → follow-ups, with strong answers, weak answers (and why they fail), and misconceptions.
- **F. Production relevance** — how it shows up in a real service and what it protects.
- **G. Revision** — 10 takeaways, 10 rapid-fire, 5 tricky, 5 exercises, one-page sheet, prerequisites to revisit.

Minor subtopics get proportionate coverage rather than the full template.

---

## Definition of "Mastered"

A topic is **Mastered** only when you can, *without notes*:
1. Explain it from first principles (what/why/what-it-replaced).
2. Implement a correct example.
3. Diagnose a realistic failure of it.
4. Compare it against alternatives with honest trade-offs.
5. Answer at least two layers of follow-up questions.

Anything short of all five is *Practicing* or *Needs Revision*. See [`PROGRESS.md`](./PROGRESS.md).

---

## Accuracy commitments

- No invented benchmark numbers, guarantees, or company-specific question histories.
- Version-specific implementation details are labeled, not presented as universal rules.
- Official docs (Java, Spring, Hibernate, PostgreSQL, Oracle) are cited where behavior is version-sensitive.
- Conceptual correctness is distinguished from production suitability.

---

## Build status of this repository

This repo is being developed in batches. **[`PROGRESS.md`](./PROGRESS.md) is the single source of truth** for which chapters are *complete*, *partial*, or *pending*. Do not assume a chapter is finished because its folder exists — check PROGRESS.md.
