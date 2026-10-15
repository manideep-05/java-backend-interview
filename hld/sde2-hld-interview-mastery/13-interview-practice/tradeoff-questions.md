# Trade-off Questions

> Related: [architecture-defense-questions](./architecture-defense-questions.md) · [14-revision/tradeoff-cheat-sheet](../14-revision/tradeoff-cheat-sheet.md)

"What's the trade-off of X?" / "X vs Y — when which?" questions. The strongest answers name what you *gain*, what you *sacrifice*, and *when* each wins — never declaring one universally better.

---

## The trade-off answer pattern

> "X gains [benefit] at the cost of [sacrifice]; Y is the opposite. Use X when [condition], Y when [condition]."

Every real decision is a trade-off; there is no universally best choice. Interviewers test whether you see both sides.

---

## Core trade-offs (Q → model answer)

**SQL vs NoSQL?**
→ SQL: rich queries + ACID + flexibility, harder to scale writes. NoSQL: horizontal scale + performance for a specific access pattern, at the cost of query flexibility and (often) transactions. SQL by default; NoSQL when a known access pattern outgrows a single node.

**Strong vs eventual consistency?**
→ Strong: correctness + simple reasoning, at higher latency and lower availability under partition. Eventual: low latency + availability + read scaling, at the cost of stale reads. Strong for money/inventory; eventual for feeds/counts. Pick the weakest that's correct.

**Normalization vs denormalization?**
→ Normalized: consistent, simple writes, join-heavy reads. Denormalized: fast reads (no joins) at the cost of write amplification + managed consistency. Normalize by default; denormalize a measured read hot path.

**Sync vs async processing?**
→ Sync: simple, immediate, strong consistency, but couples latency to slow work and can cascade. Async: fast response + decoupled + spike-absorbing + retryable, at the cost of eventual completion, duplicates (idempotency), ordering, backpressure. Sync for needed-now/fast; async for slow/heavy/unreliable.

**Push vs pull (fan-out)?**
→ Push (fan-out-on-write): fast reads, expensive writes (celebrity amplification). Pull (fan-out-on-read): cheap writes, expensive reads. Hybrid: push for normal, pull for celebrities.

**Monolith vs microservices?**
→ Monolith: simple, local transactions, scales as a unit. Microservices: independent deploy/scale/ownership, at the cost of network calls, distributed transactions, ops. Modular monolith default; decompose on team/scaling pressure.

**Horizontal vs vertical scaling?**
→ Horizontal: unbounded + redundant, needs statelessness, doesn't fix a shared DB bottleneck. Vertical: simple, no app changes, but ceiling + SPOF. Scale the stateless tier horizontally; the DB vertically → replicas → shard.

**Cache-aside vs write-through?**
→ Cache-aside: memory-efficient, resilient (DB fallback), lazy; write-through: cache consistent on write + warm reads, at per-write latency + caching possibly-unread data. Cache-aside default; write-through for read-after-write-heavy.

**Optimistic vs pessimistic locking?**
→ Optimistic: no held locks, scales when conflicts are rare, retries on conflict; pessimistic: safe under frequent conflicts, but contention/deadlocks. Optimistic for rare conflicts (web edits); pessimistic for frequent (hot rows).

**Leader-based vs leaderless replication?**
→ Leader-based (single primary): no write conflicts, simple, but write scaling limited. Leaderless (quorum): write scaling + availability, at the cost of conflict resolution. Leader-based default; leaderless for high-availability write-anywhere.

**At-least-once vs at-most-once delivery?**
→ At-least-once: never loses, may duplicate (need idempotency) — the default. At-most-once: may lose, no duplicates — only for loss-tolerant data. Business systems use at-least-once + idempotency.

**CDN/edge cache vs origin?**
→ CDN: low global latency + origin offload for static/cacheable content, at invalidation complexity + cost. Origin: dynamic/private content. Push cacheable content to the edge; keep dynamic/private at the origin.

---

## The anti-pattern: "X is just better"

If you ever say "X is faster/better/more scalable" without the cost and the condition, you've failed the trade-off. Always name the sacrifice and the switching condition. (See [common-design-mistakes](./common-design-mistakes.md).)

---

## Exercise

For 5 decisions in a design you did, state the trade-off in the pattern form ("gains X at the cost of Y; use when Z"). Then pick two where the *opposite* choice would be right and explain the condition that flips it. See the condensed table in [tradeoff-cheat-sheet](../14-revision/tradeoff-cheat-sheet.md).

**Next:** [bottleneck-and-failure-drills](./bottleneck-and-failure-drills.md)
