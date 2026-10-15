# Scaling Follow-up Questions

> Related: [00-foundations/architecture-fundamentals](../00-foundations/architecture-fundamentals.md) · [common-interviewer-pushbacks](./common-interviewer-pushbacks.md)

The "what if traffic grows 10×?" family — the most common follow-up. The strong answer re-estimates, names the *first* bottleneck, fixes it with the matching mechanism, and names the *next* bottleneck — never a blanket "scale horizontally."

---

## The scaling-follow-up pattern

> "At N×, first re-estimate which dimension that pressures. The first bottleneck becomes [X]; I'd fix it with [matching mechanism]. The next bottleneck would then be [Y]." Each step justified by a metric; introduce complexity only when forced.

---

## The question bank (Q → model answer)

**"What happens at 10× traffic?"**
→ First I'd ask *which dimension* 10× pressures (users ≠ RPS ≠ data ≠ connections) and re-estimate. Typically reads hit first → cache + replicas; then write throughput → batching/sharding; then connections → a pooler. I introduce each only when the metric forces it, not all at once. (See [architecture-fundamentals](../00-foundations/architecture-fundamentals.md).)

**"What's the first bottleneck?"**
→ Depends on the system, but usually the DB (reads first in read-heavy systems). I'd name it specifically and how I'd detect it (DB CPU/connections, replica lag), not guess.

**"Why not just add more servers?"**
→ Adding app servers scales the stateless tier but multiplies connections to the single DB, often *worsening* the bottleneck. The fix must match the bottleneck type (index/cache/replica/shard for the DB), not blanket app scaling. (See [horizontal-vs-vertical-scaling](../02-compute-and-architecture/horizontal-vs-vertical-scaling.md).)

**"When would you shard the database?"**
→ Only when a single primary's write throughput or data size is the measured ceiling — after exhausting indexing, caching, and replicas. I'd pick a shard key that keeps the common query single-shard and avoids hot partitions. (See [partitioning-and-sharding](../03-databases-and-data-modeling/partitioning-and-sharding.md).)

**"What happens when a single partition/shard becomes hot?"**
→ A hot shard means skewed load (a celebrity/big tenant/'today' range). Mitigate with a better/compound shard key, splitting the hot key, caching the hot data, or dedicated capacity — and I'd verify via per-shard metrics. (See [partitioning-and-sharding](../03-databases-and-data-modeling/partitioning-and-sharding.md).)

**"We now have 100M users — redesign?"**
→ Not from user count alone — MAU ≠ RPS ≠ data size. I'd re-estimate the pressured dimension; a read-mostly system with mostly-idle users might still run on a primary + replicas + cache. I add complexity only where a dimension is actually under pressure. (See [architecture-fundamentals](../00-foundations/architecture-fundamentals.md).)

**"How do you scale writes?"**
→ Read replicas don't help writes. Options: batch writes, a write-optimized store (LSM/wide-column), or shard writes across primaries. For a hot row, append-only + projection or sharded counters. (See [replication-and-read-replicas](../03-databases-and-data-modeling/replication-and-read-replicas.md).)

**"How do you handle a traffic spike (flash sale, launch)?"**
→ Pre-scale for known events (autoscaling lags); queue + absorb spikes for async work; load-shed low-priority; cap autoscaling at downstream (DB) capacity so you don't scale *into* a bottleneck. (See [autoscaling-and-capacity-planning](../02-compute-and-architecture/autoscaling-and-capacity-planning.md).)

**"Consumer can't keep up with producers — scale it?"**
→ Autoscale workers up to the partition count (add partitions if needed); prioritize/shed; and protect downstream during recovery so clearing the backlog doesn't cause a second outage. (See [backpressure-and-consumer-lag](../05-messaging-and-event-driven-systems/backpressure-and-consumer-lag.md).)

**"When do you go multi-region?"**
→ Only with a concrete requirement: global low-latency, surviving a full-region outage, or data residency — not user count. Multi-AZ handles most failures far more cheaply. (See [multi-region-architecture](../06-distributed-systems/multi-region-architecture.md).)

**"What's the scaling order for a read-heavy relational system?"**
→ Index/query fix → cache → read replicas → shard (writes/size). Climb a rung only when the cheaper one is exhausted (measured). (See [scaling-bottleneck-diagnosis](../11-estimation-and-bottleneck-analysis/scaling-bottleneck-diagnosis.md).)

---

## The two phrases that win scaling follow-ups

1. **"First, which dimension does that pressure?"** — forces re-estimation (users ≠ RPS ≠ data).
2. **"The first bottleneck becomes X; after I fix it, the next is Y."** — shows you understand the evolution, not a one-shot "scale horizontally."

---

## Exercise

For a design you did, have someone escalate: "10× users" → "now 100×" → "now a hot tenant" → "now a full-region outage." At each step, re-estimate the pressured dimension, name the first bottleneck + matching fix, and the next bottleneck. If you ever answer "scale horizontally" without naming the next bottleneck, redo it.

**Next:** [common-interviewer-pushbacks](./common-interviewer-pushbacks.md)
