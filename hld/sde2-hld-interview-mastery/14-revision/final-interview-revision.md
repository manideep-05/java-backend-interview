# Final Interview Revision (Night Before / Morning Of)

> The last thing to read. If you can recall this page, you have the scaffolding to drive any SDE-2 HLD interview. Everything links back to the full chapters if you need depth.

## The 60-second mental model (run this at the start of every round)

**Clarify → Estimate → API → Data model → Architecture → Data flow → Bottleneck → Scale/evolve → Consistency/failures → Security/observability → Summarize.** (See [one-page-system-design-framework](./one-page-system-design-framework.md).)

## The 5 numbers

- 86,400 s/day · **1M/day ≈ 12 RPS** · peak = avg × (2–10×)
- **Concurrency = RPS × latency** (Little's Law)
- Storage = daily × retention × ×3 replication
- 95% cache hit → 20× fewer DB reads
- RAM ~100× SSD ~100× disk; cross-continent RTT ~100ms

## The decision defaults (when in doubt, say these)

- **Modular monolith** → decompose on measured need.
- **Relational DB** → replicas → shard (writes/size); NoSQL for a known access pattern at scale.
- **Scaling ladder:** index/query → cache → replica → shard.
- **Sync** for needed-now; **async** (queue + workers + idempotency + DLQ) for slow/heavy.
- **Weakest consistency that's correct** per path.
- **At-least-once + idempotency** = effectively-once.
- **Files → object storage** + metadata in DB + CDN.
- **Single-region multi-AZ** → multi-region only on a real requirement.

## The 10 things that signal SDE-2 maturity

1. Clarify before designing; find the **dominant NFR**.
2. Estimate with shown math; **numbers justify the design (including simplicity)**.
3. Default to the **simplest adequate** design; justify every component with a number.
4. For each decision, state the **trade-off** (gain + cost + switching condition).
5. **Idempotency** for retried/async operations (the thread through everything).
6. **"Exactly-once delivery is a myth"** → effectively-once via idempotency + reconciliation.
7. **Diagnose bottlenecks from signals**, not "add servers."
8. **Match the fix to the bottleneck type**; know the next bottleneck.
9. **Graceful degradation / failure paths**, not just the happy path.
10. **Revise gracefully** under pushback; reason from principles when you don't know something.

## The 10 mistakes that fail SDE-2

Over-engineering (complexity with no number) · happy-path-only · microservices/Kafka/sharding/multi-region without justification · "NoSQL because faster" · check-then-charge · trusting a lock for correctness (no fencing) · claiming exactly-once · ignoring p99 · ignoring connection limits · no idempotency on retries. (See [common-design-mistakes](../13-interview-practice/common-design-mistakes.md).)

## Pushback one-liners

- "Won't scale" → re-estimate, name first + next bottleneck.
- "Over-engineered" → justify or remove each component.
- "Component down" → graceful degradation (fallback).
- "Stronger consistency" → primary/quorum read for that path + cost.
- "Exactly-once?" → effectively-once (at-least-once + idempotency + reconciliation).
- "Don't know X" → reason from first principles, honestly.

## The core design patterns to have ready

- **Idempotency key** (atomic check-and-record before the effect) — payments, creates, consumers.
- **Transactional outbox / CDC** — reliable event publishing (no dual-write); feeds search/cache/services.
- **Cache-aside + invalidation + Redis-down fallback**.
- **Hybrid fan-out** (push normal, pull celebrities) — feeds.
- **Saga + compensation** — cross-service workflows (not 2PC).
- **Atomic conditional update** — no oversell / no double-book.
- **Timeout + circuit breaker + bulkhead + degradation** — resilience trio.
- **Reconciliation** — the honest correctness backstop for money.

## The running example (if you blank)

The **DMS** ([document-management-system](../12-system-design-problems/document-management-system.md)) ties it all together: estimation (files→object storage, metadata→SQL), async processing (idempotent OCR), outbox/CDC (search/cache/notify), read-your-write, multi-tenant isolation, and the full scaling journey — all while resisting over-engineering at modest scale.

## Final checklist before you walk in

- [ ] I can run the 11-step flow from memory in ~45 min.
- [ ] I can estimate RPS/storage in <2 min with shown math.
- [ ] I can state a trade-off for every decision.
- [ ] I can turn a check-then-act into an idempotent flow.
- [ ] I can diagnose a bottleneck from signals, not guess.
- [ ] I can describe graceful degradation for a down component.
- [ ] I can handle "10×", "stronger consistency", "component down", "over-engineered" pushbacks.
- [ ] I know the dominant NFR for the common problems.
- [ ] I will clarify first and summarize last.

Breathe. Clarify. Estimate. Keep it simple. Narrate your reasoning. Defend with requirements, not dogma. You've got this.

---
*End of the repository. Return to [README](../README.md) · [ROADMAP](../ROADMAP.md) · [INTERVIEW-CHECKLIST](../INTERVIEW-CHECKLIST.md).*
