# SDE-2 HLD Interview Mastery

A practical, example-driven learning system and reference for **High-Level Design (HLD)** interviews at product-based companies (Salesforce, Amazon, Microsoft, Atlassian, Uber, and similar). Built for a backend engineer with Java / Spring Boot experience targeting SDE-2 / Backend Software Engineer roles (₹33 LPA+ band).

This is **not** a textbook to read once. It is a course + reference + practice gym. You learn a concept, explain it aloud, estimate with it, use it in a design, then defend it under follow-up questioning.

---

## What makes this different

Every major concept answers the same questions, in order:

1. What problem are we solving, and what happens if we do nothing?
2. How did engineers solve it before this pattern existed?
3. How does it work internally, and how does data flow?
4. What are the alternatives and their trade-offs?
5. What new problems does the solution introduce?
6. What breaks at scale, and how do we detect / mitigate / prevent it?
7. How do I explain and defend this in an SDE-2 interview?

If a document ever gives you "scale horizontally as needed" without telling you *how*, *why*, and *what the next bottleneck is* — that is a bug. Report it.

---

## How the repository is organized

| Folder | What it covers | When to read |
|---|---|---|
| `00-foundations/` | What HLD is, requirements, latency/throughput, estimation, interview communication | First |
| `01-networking-and-api-design/` | DNS, TCP, TLS, HTTP, REST, auth, gateways, rate limiting, async-request & bulk APIs | Early |
| `02-compute-and-architecture/` | Monolith vs microservices, scaling, stateless services, service discovery | Early |
| `03-databases-and-data-modeling/` | SQL vs NoSQL, indexing, transactions, replication, sharding, consistency, geospatial & probabilistic structures | Core |
| `04-caching/` | Cache patterns, invalidation, stampedes, Redis failure modes | Core |
| `05-messaging-and-event-driven-systems/` | Queues, Kafka, delivery semantics, idempotency, outbox, saga, CQRS | Core |
| `06-distributed-systems/` | Partial failures, CAP, quorums, locks, exactly-once, multi-region | Core |
| `07-reliability-and-resilience/` | Availability, failover, circuit breakers, DR, SLIs/SLOs | Operational |
| `08-security/` | AuthN/AuthZ, JWT/OAuth2, secrets, encryption, tenant isolation | Operational |
| `09-observability-and-operations/` | Logs/metrics/traces, incident response, deployments | Operational |
| `10-storage-and-content-delivery/` | Object storage, CDN, search, data pipelines | Operational |
| `11-estimation-and-bottleneck-analysis/` | Reusable estimation formulas and diagnosis playbooks | Reference |
| `12-system-design-problems/` | Full worked designs (URL shortener → payment system) | Practice |
| `13-interview-practice/` | Drills, pushbacks, mistakes, mock interviews | Practice |
| `14-revision/` | One-page frameworks and cheat sheets | Before interview |
| `15-practical-backend-examples/` | Java/Spring code for the HLD patterns (idempotency, outbox, retries, DLQ, optimistic locking, circuit breaker, etc.) | Reference / mixed HLD-LLD |

Top-level files:

- **`YOUR-RESUME-STUDY-GUIDE.md`** — maps *your* experience (notification platform, Oracle optimization, cross-system sync, workflows, JobFinder) to the exact chapters, "tell it as HLD" scripts, and likely follow-ups. **Start here if you want a personalized path.**
- **`LLD-OUT-OF-SCOPE.md`** — the HLD/LLD boundary: what this repo does *not* cover (object-oriented design, SOLID, patterns, machine coding) and how to prepare the separate LLD round.
- **`ROADMAP.md`** — the ordered study path with two tracks (A: core readiness, B: advanced depth), prerequisites, and exit criteria per stage.
- **`PROGRESS.md`** — checkboxes for everything you must study, explain aloud, and practice.
- **`INTERVIEW-CHECKLIST.md`** — the readiness rubric (1–5 scale) and final self-assessment.
- **`GLOSSARY.md`** — definitions of every term used, so no document assumes undefined jargon.

---

## How to use it

**If you have 3+ months:** follow `ROADMAP.md` Track A fully, then Track B.

**If you have 4–6 weeks:** Track A only — foundations → estimation → databases → caching → messaging → distributed systems basics → 6 core design problems → mocks.

**If you have 1 week (cram):** `14-revision/` + 4 mock interviews + re-read `00-foundations/back-of-the-envelope-estimation.md` and `12-system-design-problems/` for URL shortener, rate limiter, notification service, chat system.

### The study loop (per topic)

1. **Read** the document.
2. **Explain aloud** — close the file, explain the concept and its trade-offs to an imaginary interviewer in 2–3 minutes. If you can't, re-read.
3. **Estimate** — do the hands-on exercise and any estimation drill.
4. **Apply** — find the concept inside one of the `12-system-design-problems/` designs.
5. **Defend** — answer the follow-up questions in the doc and in `13-interview-practice/`.
6. **Track** — tick the box in `PROGRESS.md`. Only tick "explained aloud" when you actually did it without notes.

> A file existing is not progress. Being able to explain and defend it is.

---

## Conventions used throughout

- **Mermaid** for architecture, sequence, flow, and state diagrams. If your Markdown viewer doesn't render Mermaid, use the VS Code "Markdown Preview Mermaid Support" extension or paste into [mermaid.live](https://mermaid.live).
- **Running example:** a **Document Management System (DMS)** — users, jobs, documents, revisions, files, notifications, access permissions. Secondary examples pull from e-commerce, chat, payments, social feeds, and ride-hailing.
- **Heuristics vs laws:** anything like "a single PostgreSQL node handles X" is labeled a *heuristic* (order-of-magnitude, workload-dependent), never a universal law. Always validate with measurement.
- **Java / Spring Boot** for code examples, but architectural reasoning stays language-independent.
- **SDE-2 scope markers:** topics are tagged where expectations differ between SDE-1, SDE-2, and Staff. You are not required to design a global multi-region system to pass SDE-2 — you are required to know *when* you would and *why*.

---

## What "done" looks like

When you finish this repo and its exercises, you should be able to take an unfamiliar HLD problem and:

- Clarify functional / non-functional requirements and scope.
- Estimate scale (RPS, storage, bandwidth, connections) with shown math.
- Propose the **simplest** architecture that meets the requirements.
- Design APIs and a data model.
- Trace critical data flows including failure paths.
- Identify the first bottleneck and the next one after you fix it.
- Justify technology choices against alternatives.
- Reason about consistency, concurrency, and partial failure.
- Defend trade-offs under 10× traffic / stronger-consistency / component-down pushbacks.

Start with [`ROADMAP.md`](./ROADMAP.md).
