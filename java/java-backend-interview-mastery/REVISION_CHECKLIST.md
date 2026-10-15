# Final Revision Checklist

Use this in the last 1–2 weeks and the night before. Each item is phrased as something you must be able to **do**, not just recognize. If you can't do it from memory, mark the topic *Needs Revision* in `PROGRESS.md`.

> **Full-loop reminder:** this checklist covers the backend knowledge + behavioral + DSA rounds (DSA is now in ch.31). A complete SDE-2 loop also has **LLD and HLD rounds — prepared separately**. Confirm those are solid; they often carry equal or greater weight than the knowledge round.

---

## Behavioral round (ch.29) — do not neglect this
- [ ] 6–8 STAR stories written, each with 2 rehearsed follow-ups (use the B1–B7 seeds).
- [ ] One honest **failure** story that ends with a prevention step.
- [ ] One **conflict/disagreement** story that ends in a decision.
- [ ] One **mentoring** story (you mentored 3 trainees — use it).
- [ ] Every story has a defensible number; you lead with "I", not just "we".
- [ ] "Why SDE-2 / why this company" + 2–3 questions to ask the interviewer.

## Node.js (ch.30) — only if you'll be interviewed on Node
- [ ] Event-loop task ordering (nextTick → Promise → setTimeout) and why CPU work blocks everything.
- [ ] Scale cores with cluster/PM2; worker_threads for CPU; streams + backpressure.
- [ ] Articulate Node vs JVM and that your reliability patterns (outbox/retry/DLQ/idempotency) are language-independent.

## DSA (ch.31) — pattern recognition under pressure
- [ ] From a cold prompt, name the pattern and *why* within ~30s (use the identification table / cheatsheet).
- [ ] Recall each pattern's template from memory (two-pointer, sliding window, BFS/DFS, DP state, backtracking choose/recurse/undo, union-find).
- [ ] State brute force + its O() before optimizing; always give time AND space (incl. recursion-stack O(h)).
- [ ] Know the pattern tie-breakers: sum=K with negatives→prefix sum; "all solutions"→backtracking vs "count/optimum"→DP; undirected cycle→union-find vs directed→topo/DFS.
- [ ] Finish all **P0** problems in `31-dsa-patterns/question-bank.md`; be able to code the DP recipe (state→transition→base→order→answer→space).
- [ ] Dry-run a small input out loud and handle edge cases (empty, single, duplicates, overflow with `long`).

---

## Tier 0 — do not walk in without these

### Concurrency (ch.06)
- [ ] Explain atomicity vs visibility vs ordering, and why `volatile` fixes visibility but not `i++`.
- [ ] State one happens-before rule and why it matters.
- [ ] Fix a broken counter with `AtomicLong` and with a lock, and say which you'd pick and why.
- [ ] Explain why a larger thread pool can reduce throughput.
- [ ] Describe thread-pool saturation and the four `RejectedExecutionHandler` policies.
- [ ] Reproduce and prevent a lock-ordering deadlock.
- [ ] Say when virtual threads help (IO-bound, high concurrency) and when they don't (CPU-bound), and what pinning is.

### Spring transactions (ch.13)
- [ ] Explain `@Transactional` as a proxy and why self-invocation / non-public methods silently do nothing.
- [ ] State the default rollback rule and how checked exceptions change it.
- [ ] Explain why a transaction doesn't follow an `@Async` call to another thread.
- [ ] Choose between REQUIRED and REQUIRES_NEW with a concrete example.

### JPA / Hibernate (ch.12)
- [ ] Explain the persistence context, dirty checking, and flush vs commit.
- [ ] Show how N+1 arises and fix it with a fetch join / entity graph / projection; name the trade-off of each.
- [ ] Choose optimistic (`@Version`) vs pessimistic locking by contention profile and handle the retry.

### Collections (ch.03)
- [ ] Walk HashMap put: hash → bucket → chain → treeify, and resize behavior.
- [ ] Explain why HashMap isn't thread-safe and why ConcurrentHashMap still can't make compound ops atomic.
- [ ] Explain fail-fast iteration and when CME is / isn't thrown.

### DB transactions & isolation (ch.17)
- [ ] Map each isolation level to the anomalies it permits.
- [ ] Prevent a lost update two ways and compare them.
- [ ] Explain MVCC in one minute.
- [ ] Describe how you'd investigate a deadlock in Postgres and in Oracle.

### SQL & indexing (ch.15, 16)
- [ ] Write a window-function query (ranking + running total) from memory.
- [ ] Explain EXISTS vs IN including the NULL trap.
- [ ] Explain why a composite index needs the right leading column and when the optimizer ignores it.
- [ ] Define sargability and give two non-sargable predicates.
- [ ] Read an `EXPLAIN ANALYZE` and justify an index change.

### Core Java (ch.01)
- [ ] State the equals/hashCode contract and what breaks if violated.
- [ ] Explain the Integer cache `==` trap.
- [ ] Explain why String is immutable and how the pool works.
- [ ] Build a correct immutable value type with defensive copies.

### Production debugging (ch.22)
- [ ] Give the generic workflow: symptoms → hypotheses → evidence → root cause → fix → verify → prevent.
- [ ] Walk the "endpoint slow but query fast" and "connection-pool exhaustion" playbooks.

---

## Tier 1 — strong signal
- [ ] Spring DI internals; constructor vs field injection trade-offs (ch.09).
- [ ] Request lifecycle: filter → DispatcherServlet → controller → service → repo (ch.11).
- [ ] Streams: map vs flatMap, reduce vs collect, when a loop is clearer (ch.08).
- [ ] Optional: correct use vs misuse (ch.08).
- [ ] PECS and type erasure (ch.04).
- [ ] try-with-resources and suppressed exceptions (ch.05).
- [ ] GC basics; OOM vs StackOverflow; how to read a heap dump (ch.07).
- [ ] Idempotency keys vs idempotent methods; retries/backoff/jitter (ch.18).
- [ ] JWT pitfalls; authn vs authz; method security (ch.14, 25).
- [ ] At-least-once delivery + idempotent consumer; outbox; DLQ (ch.19).
- [ ] Cache-aside + invalidation; stampede; stale-data correctness (ch.20).
- [ ] `@WebMvcTest` vs `@SpringBootTest`; Testcontainers (ch.21).
- [ ] TLS handshake; keep-alive; timeouts (ch.23).

---

## Communication rubric (practice saying these well)
- [ ] I **clarify** before answering scenario questions (ask about scale, consistency needs, failure tolerance).
- [ ] I state **assumptions and version dependence** explicitly.
- [ ] I give the **trade-off**, not just the answer ("X is simpler but Y scales; I'd start with X because…").
- [ ] I distinguish **"the language guarantees"** from **"this implementation happens to"**.
- [ ] I admit the boundary of what I'd **verify with evidence** rather than claiming certainty.

---

## Night-before sweep (30 minutes)
Read only the one-page revision sheets in each chapter's §G and in `revision/`. Do not learn anything new. Sleep.
