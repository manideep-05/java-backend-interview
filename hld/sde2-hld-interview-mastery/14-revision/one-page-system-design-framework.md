# One-Page System Design Framework

> The whole method on one page. For the full treatment: [interview-communication-framework](../00-foundations/interview-communication-framework.md).

## The 11 steps (45-min budget)

1. **Clarify** (5 min) — FRs + 6 NFR questions (scale, latency, availability, consistency, durability, read:write) + assumptions + scope/non-goals + confirm.
2. **Estimate** (4) — DAU → actions/day → avg RPS → peak RPS → read/write split → storage → bandwidth. Show math, state implication.
3. **API** (3) — 3–5 endpoints (methods, key errors, idempotency, pagination).
4. **Data model** (4) — core entities, keys, key indexes, what must be consistent.
5. **Architecture** (6) — simplest adequate diagram; justify each component.
6. **Data flow** (5) — one write + one read + a failure path.
7. **Bottlenecks** (3) — first bottleneck + how to detect.
8. **Scale/evolve** (5) — add replica/cache/queue/shard only when justified; next bottleneck.
9. **Consistency/failures** (5) — timeouts, retries, idempotency, degradation; what consistency per path.
10. **Security/observability** (3) — authz boundary, tenant isolation, key metrics/alerts.
11. **Summarize** — recap design + trade-offs + top risks.

## The core numbers

- 86,400 s/day; **1M/day ≈ 12 RPS**; peak = avg × (2–10×).
- Little's Law: **concurrency = RPS × latency** (sizes pools).
- Storage = daily × retention × replication (×3).
- 95% cache hit → 20× fewer DB reads.
- RAM ~100× SSD ~100× disk-seek; cross-continent RTT ~100ms.

## The decision defaults (simplest-first)

- Architecture: **modular monolith** → decompose on need.
- DB: **relational** → replicas → shard (writes/size); NoSQL for a known access pattern at scale.
- Scaling ladder (DB reads): **index/query → cache → replica → shard**.
- Processing: **sync** for needed-now; **async** for slow/heavy/unreliable (then idempotency + DLQ).
- Consistency: **weakest that's correct** per path (strong for money/inventory; eventual for feeds/counts).
- Delivery: **at-least-once + idempotency** = effectively-once.
- Files: **object storage** + metadata in DB + CDN.
- Deployment: single-region **multi-AZ** → multi-region only on a real requirement.

## The strong-statement pattern

> "I'd start with **[simple]** because [requirement]. I'd add **[next]** when [measurable trigger]. I'd only consider **[complex]** after [condition]."

## The habits

Narrate reasoning · state a trade-off per decision · default simple, justify additions with a number · think aloud · check in at decision points · revise gracefully when challenged · summarize at the end.

## Pushback responses (one-liners)

- "Won't scale" → re-estimate; name first bottleneck + fix + next bottleneck.
- "Over-engineered" → justify each component or remove it.
- "Component down" → describe graceful degradation (fallback), not failure.
- "Stronger consistency" → read from primary/quorum for that path + name the cost.
- "Exactly-once?" → effectively-once via at-least-once + idempotency (+ reconciliation).
- "Don't know X" → reason from first principles honestly.

## The two mistakes to avoid

**Over-engineering** (complexity with no justifying number) · **happy-path thinking** (ignoring timeouts/retries/duplicates/concurrency).

---
**Next:** [tradeoff-cheat-sheet](./tradeoff-cheat-sheet.md)
