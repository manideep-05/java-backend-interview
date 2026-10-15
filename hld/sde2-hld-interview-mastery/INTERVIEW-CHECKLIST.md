# Interview Readiness Checklist & Rubric

Use this to **measure** readiness, not to feel ready. Score yourself honestly after mock interviews. Target for SDE-2: **average ≥ 3.5/5**, with no single dimension below 3.

---

## Pre-interview quick checklist (the 60-second mental model)

Before any HLD round, you should be able to run this without thinking:

1. **Clarify** — functional + non-functional requirements, scope boundaries, who the users are.
2. **Estimate** — DAU → RPS → peak; storage/year; read:write ratio.
3. **API** — the 3–5 endpoints that define the product.
4. **Data model** — core entities, keys, indexes, what must be consistent.
5. **Architecture** — simplest diagram that works; name each component's reason to exist.
6. **Data flow** — walk one write and one read, including a failure path.
7. **Bottleneck** — where it breaks first; how you'd detect it.
8. **Scale/evolve** — introduce replica / cache / queue / shard *only when justified*.
9. **Failure & consistency** — timeouts, retries, idempotency, what consistency you actually need.
10. **Security & observability** — authz boundary, sensitive data, key metrics/alerts.
11. **Trade-offs** — state what you gave up and why.

---

## The rubric (1–5 per dimension)

For each dimension: **1 = weak, 2 = developing, 3 = interview-ready (SDE-2 bar), 4 = strong, 5 = standout.**

### 1. Requirement clarification
- **1:** Jumps to architecture without clarifying.
- **2:** Asks a few questions, misses non-functional needs.
- **3:** Separates functional/non-functional, sets scope and assumptions explicitly.
- **4:** Clarifications visibly shape the design; surfaces hidden constraints (consistency, multi-tenancy).
- **5:** Reframes ambiguous problems into a crisp, bounded spec and confirms with interviewer.

### 2. Capacity estimation
- **1:** No numbers, or hand-waved.
- **2:** Some numbers, no shown math, not used later.
- **3:** Order-of-magnitude RPS/storage with shown math, ties to design decisions.
- **4:** Peak vs average, read:write, growth over years; uses numbers to pick components.
- **5:** Sanity-checks estimates, flags assumptions and sensitivity.

### 3. API & data modeling
- **1:** Vague endpoints, no schema.
- **2:** Endpoints listed, schema incomplete, no keys/indexes.
- **3:** Clear APIs (methods/paths/errors), core entities with keys and key indexes.
- **4:** Idempotency, pagination, consistency needs reflected in the model.
- **5:** Access patterns drive the schema; partitioning/index choices justified.

### 4. Architecture selection
- **1:** Random boxes, or over-engineered from the start.
- **2:** A reasonable diagram but can't justify components.
- **3:** Simplest adequate design; each component justified; alternatives acknowledged.
- **4:** Chooses between ≥2 architectures using stated assumptions.
- **5:** Clean evolution path; avoids premature complexity while anticipating it.

### 5. Data flow
- **1:** Can't trace a request end-to-end.
- **2:** Traces happy path only.
- **3:** Traces a write and a read through components.
- **4:** Includes at least one failure path.
- **5:** Sequence diagrams with normal + failure + recovery.

### 6. Bottleneck diagnosis
- **1:** "Add more servers."
- **2:** Identifies one bottleneck.
- **3:** Identifies first bottleneck and how to detect it.
- **4:** Knows the next bottleneck after the first fix.
- **5:** Diagnoses from metrics/traces, not guessing; distinguishes DB vs app vs cache vs network.

### 7. Consistency & concurrency
- **1:** Ignores it.
- **2:** Mentions it vaguely.
- **3:** States what consistency the system actually needs and where.
- **4:** Handles concurrent writes (optimistic locking, idempotency keys).
- **5:** Reasons about anomalies, quorum/replica trade-offs, and chooses deliberately.

### 8. Failure handling
- **1:** Happy path only.
- **2:** Mentions retries.
- **3:** Timeouts, retries with backoff, idempotency, DLQ.
- **4:** Partial failure, duplicate execution, graceful degradation.
- **5:** Blast-radius thinking, recovery/reconciliation, correlated failure.

### 9. Security & observability
- **1:** Ignores both.
- **2:** Mentions auth.
- **3:** Authz boundaries, sensitive data, core metrics/logs/alerts.
- **4:** Tenant isolation, secrets, rate limiting, tracing.
- **5:** Threat-models abuse paths; defines SLIs/SLOs.

### 10. Trade-off communication
- **1:** States choices as facts.
- **2:** Names one trade-off.
- **3:** For key decisions, states gain + cost + alternative.
- **4:** Ties trade-offs back to requirements.
- **5:** Changes decision gracefully when requirements change, explaining impact.

### 11. Interview time management
- **1:** Runs out of time on requirements.
- **2:** Spends too long in one area.
- **3:** Covers clarify → estimate → design → scale → failures in ~45 min.
- **4:** Adapts allocation to interviewer's interest.
- **5:** Leaves time for a crisp summary and invites follow-ups.

---

## Score sheet (copy per mock)

| Dimension | Score (1–5) | Note |
|---|---|---|
| Requirement clarification |  |  |
| Capacity estimation |  |  |
| API & data modeling |  |  |
| Architecture selection |  |  |
| Data flow |  |  |
| Bottleneck diagnosis |  |  |
| Consistency & concurrency |  |  |
| Failure handling |  |  |
| Security & observability |  |  |
| Trade-off communication |  |  |
| Time management |  |  |
| **Average** |  |  |

**Readiness bands:**
- **< 2.5** — Weak: revisit foundations, databases, and estimation.
- **2.5–3.4** — Developing: do more full-length designs under timer; focus on failure handling and trade-offs.
- **3.5–4.4** — Interview-ready (SDE-2): schedule interviews; keep practicing weak dimensions.
- **≥ 4.5** — Strong: target higher bands / harder companies; move into Track B depth.

---

## Final self-assessment — 5 unseen design variations

Do these **without reading any solution first**. Time-box each to 40 minutes, then score with the rubric. These are intentionally not in the `12-system-design-problems/` library so you can't memorize them.

1. **Design a "read-it-later" service** (save article URL, fetch & store readable text, full-text search across saved articles, offline sync to mobile). Where's the consistency boundary? What breaks at 10M saved articles?
2. **Design a coupon / promo-code system for an e-commerce checkout** (limited-use codes, per-user limits, no overuse under concurrency, flash-sale spikes). This tests concurrency and overselling.
3. **Design a server-side feature-flag service** (flags evaluated per request at low latency across 500 services, gradual rollout %, instant kill-switch). Tests caching, propagation latency, consistency vs availability.
4. **Design an email-digest system** (aggregate each user's daily activity, send one summary email at the user's local 8 AM, 50M users across time zones). Tests scheduling, fan-out, time-zone sharding, idempotent sends.
5. **Design a "typing indicator + presence" subsystem for a chat app** (who's online, who's typing, at massive fan-out). Tests real-time delivery, ephemeral state, write amplification.

For each: did you clarify, estimate, design, find the bottleneck, and handle failure within time? If any dimension < 3, log it in `PROGRESS.md` Part 8 and revisit the relevant chapter.
