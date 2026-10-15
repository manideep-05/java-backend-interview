# Queue Capacity and Consumer Throughput

> Prerequisites: [05-messaging/backpressure-and-consumer-lag](../05-messaging-and-event-driven-systems/backpressure-and-consumer-lag.md), [estimation-cheat-sheet](./estimation-cheat-sheet.md)
> Related: [02-compute-and-architecture/autoscaling-and-capacity-planning](../02-compute-and-architecture/autoscaling-and-capacity-planning.md)

Sizing async pipelines: how many workers to keep up with producers, how fast a backlog grows, and when it overflows retention. The throughput-dimension estimation.

---

## A. Formulas

- **Workers needed** = `producer rate / per-worker throughput` (+ headroom for spikes/retries).
- **Lag growth rate** = `producer rate − total consumer rate` (positive → backlog grows).
- **Time to drain a backlog** = `backlog size / (consumer rate − producer rate)` (consumer must exceed producer to drain).
- **Time to retention overflow** = `(retention capacity − current backlog) / lag growth rate`.
- **Kafka parallelism cap** = `partition count` (consumers beyond this idle — see [kafka-internals-and-partitioning](../05-messaging-and-event-driven-systems/kafka-internals-and-partitioning.md)).

---

## B. Worked example — notification pipeline

Producers 30K msg/s; each worker 5K/s; topic has 8 partitions; retention 6 hours.
- **Workers to keep up:** 30K/5K = 6 workers (+ headroom → ~8). Fits within 8 partitions ✓.
- **If a spike hits 50K/s** (still 8 workers = 40K/s capacity): lag grows at 50K − 40K = **10K/s**.
- **Retention capacity** ≈ 40K/s × 6h × 3,600 ≈ ~860M messages. At 10K/s growth, time to overflow ≈ 860M / 10K ≈ **~24 hours** — enough time to react (add partitions + workers), but you must act.
- **Recovery:** add partitions (say to 12) + 12 workers = 60K/s; now draining at 60K − 50K = 10K/s; a 100M backlog drains in 100M/10K ≈ **~2.8 hours** — but ensure downstream (DB/provider) can take 60K/s (don't cause a second outage — see [backpressure-and-consumer-lag](../05-messaging-and-event-driven-systems/backpressure-and-consumer-lag.md)).

---

## C. Key insights

- **Capacity = partitions × per-worker throughput** (in Kafka) — adding workers beyond partitions does nothing; add partitions first.
- **If producers persistently exceed max consumer capacity, no buffer saves you** — the backlog grows until retention overflow (data loss). You must add capacity, shed load, or slow producers (backpressure).
- **Recovery must respect downstream capacity** — fast consumers hammer the DB/provider; the pipeline's effective throughput is bounded by the slowest downstream (the minimum-across-tiers rule).
- **Headroom** matters: size workers for peak + retries, not average.

---

## D. Interview Q&A

- Q: "Producers 20K/s, workers do 2K/s each, 6 partitions. Keep up?" → *Max 6 workers (= partitions) × 2K = 12K/s < 20K/s → lag grows. Add partitions (to ≥10) + workers to reach ≥20K/s capacity.*
- Q: "Backlog is 10M, producer 5K/s, you can run consumers at 15K/s. Drain time?" → *Drain rate = 15K − 5K = 10K/s; 10M/10K = ~1,000s ≈ ~17 min — if downstream can take 15K/s.*
- Q: "Why not just add 100 consumers to clear a backlog fast?" → *Capped by partition count (extras idle), and overwhelming downstream (DB/provider) causes a second outage. Scale to partitions, limit downstream concurrency, ramp.*

**Common wrong answers:** adding consumers beyond partitions; ignoring downstream capacity in recovery; assuming the queue absorbs a persistent producer>consumer imbalance.

---

## E. Exercise

An image-processing pipeline: uploads spike to 10K/s, each worker processes 1 image/sec, topic has 500 partitions, object-storage write limit ~8K/s, retention 2 hours. Compute: workers to keep up, whether downstream (object storage) is the real bottleneck, lag growth if you max workers, and your plan.
*(Answer: need 10K workers to match 10K/s, within 500 partitions? No — only 500 consumers can work (= partitions), so max 500 img/s unless partitions increase. Even so, object storage caps at 8K/s → the real bottleneck is downstream, not workers. Plan: increase partitions + workers toward 8K/s (object-storage limit), shed/deprioritize if uploads exceed 8K/s sustained, and apply backpressure — adding workers past 8K/s just overwhelms object storage.)*

---

## F. Revision summary

**Key takeaways**
- Workers needed = producer rate / per-worker throughput (+ headroom); capacity capped by partition count (Kafka).
- Lag grows at producer − consumer rate; a persistent imbalance overflows retention (data loss) — add capacity / shed / slow producers.
- Backlog drain time = backlog / (consumer − producer); recovery is bounded by downstream capacity (don't cause a second outage).
- Effective pipeline throughput = minimum across tiers (workers, downstream).

**Rapid-fire**
1. Workers to keep up = ? → producer rate / per-worker throughput.
2. Kafka parallelism cap? → Partition count.
3. Persistent producer > consumer → ? → Backlog overflows retention (loss).
4. Drain time = ? → backlog / (consumer − producer rate).
5. Recovery bounded by? → Downstream capacity (minimum across tiers).

**Next:** [cache-sizing-and-hit-ratio](./cache-sizing-and-hit-ratio.md)
