# Estimation Cheat Sheet (with 20+ Drills)

> Prerequisites: [00-foundations/back-of-the-envelope-estimation](../00-foundations/back-of-the-envelope-estimation.md)
> Related: all `11-estimation-and-bottleneck-analysis/` chapters

This is the quick-reference + drill set. The *why* and routine are in [back-of-the-envelope-estimation](../00-foundations/back-of-the-envelope-estimation.md); this page is the numbers to memorize and 20+ progressively harder exercises with full solutions.

---

## The numbers to memorize

**Time:**
- Seconds/day ≈ **86,400 ≈ 10⁵** (use ~100,000).
- Seconds/month ≈ **2.5 × 10⁶**; seconds/year ≈ **3 × 10⁷**.

**The master shortcut:**
> **1 million requests/day ≈ 12 RPS** → **N million/day ≈ 12N RPS** (average).

**Data sizes:** KB=10³, MB=10⁶, GB=10⁹, TB=10¹², PB=10¹⁵.

**Latency (order of magnitude, heuristics):** RAM ~100ns, SSD random read ~100µs, datacenter RTT ~0.5ms, cross-continent RTT ~100ms. (RAM ~100× SSD ~100× disk-seek.)

**Rough capacities (HEURISTICS — validate):** RDBMS simple reads: thousands–low-tens-of-thousands QPS; RDBMS durable writes: low thousands/sec; Redis: ~100K ops/sec/node; app instance (I/O-bound): hundreds–low-thousands RPS.

**Multipliers:** peak = average × **peak factor** (2–10×); storage × **replication** (×3 typical) × **retention** (years).

**Little's Law:** `concurrency = RPS × latency` (sizes threads/connections).

---

## The routine (recap)

anchor (DAU/users) → actions/day → **average RPS** (÷86,400 or ×12/million) → **peak RPS** (×peak factor) → **read/write split** → **storage** (writes/day × size × retention × replication) → **bandwidth** (RPS × payload) → **derived** (instances, connections, workers, cache).

Round aggressively; numbers justify design decisions (including simplicity).

---

## Drills 1–5: Traffic / RPS

**Drill 1.** 3M requests/day, average RPS?
*Solution:* 3M/day ≈ 12×3 = **~36 RPS**.

**Drill 2.** 50M DAU, 10 actions/user/day, average RPS?
*Solution:* 500M/day ≈ 12×500 = **~6,000 RPS** average.

**Drill 3.** From Drill 2, peak factor 4× — peak RPS?
*Solution:* 6,000 × 4 = **~24,000 RPS** peak.

**Drill 4.** 2M DAU, 30 actions/day, 90% reads, 5× peak. Peak read RPS?
*Solution:* 60M/day ≈ 720 avg RPS; ×5 = 3,600 peak; ×0.9 = **~3,240 read RPS**.

**Drill 5.** A service must handle 10,000 peak RPS; each instance does ~500 RPS. Instances needed (with N+1 failover headroom)?
*Solution:* 10,000/500 = 20; +headroom (round up for failover/spikes) → **~24–25 instances**.

---

## Drills 6–10: Storage / retention

**Drill 6.** 100K users upload 5 docs/month, 2MB each. Monthly file storage?
*Solution:* 500K docs × 2MB = **~1 TB/month**.

**Drill 7.** From Drill 6, 5-year file storage with ×3 replication?
*Solution:* 1 TB × 60 months = 60 TB; ×3 = **~180 TB**.

**Drill 8.** 10M photos/day, 1.5MB each, ×3 replication, 2-year retention. Total storage?
*Solution:* 15 TB/day; ×3 = 45 TB/day; ×730 days ≈ **~33 PB**.

**Drill 9.** 50M chat messages/day, 200 bytes each, 1-year retention, ×3 replication. Message storage/year?
*Solution:* 50M×200B = 10 GB/day; ×365 = 3.65 TB; ×3 ≈ **~11 TB/year**.

**Drill 10.** Metadata records 1KB each, 500K created/month, 5-year retention. Metadata size (×3)?
*Solution:* 500K×1KB = 500 MB/month; ×60 = 30 GB; ×3 = **~90 GB** (fits one DB — justifies no sharding for metadata).

---

## Drills 11–15: Bandwidth / cache

**Drill 11.** 1,000 read RPS, 500-byte responses. Egress bandwidth?
*Solution:* 1,000 × 500B = **~0.5 MB/s** (tiny).

**Drill 12.** 24,000 peak RPS, 2KB responses. Egress?
*Solution:* 24,000 × 2KB = **~48 MB/s** (~384 Mbps).

**Drill 13.** 100M short URLs, ~100-byte cache entry each. Cache size to hold all?
*Solution:* 100M × 100B = **~10 GB** (one Redis node).

**Drill 14.** 80/20 rule: from Drill 13, caching the hot 20% captures ~80% of reads. Hot-set cache size?
*Solution:* 20M × 100B = **~2 GB** captures most reads — cache the working set, not everything.

**Drill 15.** 10,000 read RPS, want 95% cache hit ratio. DB read RPS after caching?
*Solution:* 5% miss → 10,000 × 0.05 = **~500 DB read RPS** (20× reduction).

---

## Drills 16–20: Connections / queue / latency / workers

**Drill 16.** 2,000 RPS, 20ms latency. In-flight concurrency (Little's Law)?
*Solution:* 2,000 × 0.02 = **100 in flight** (size pools accordingly + headroom).

**Drill 17.** From Drill 16, latency jumps to 200ms (DB slow). New concurrency?
*Solution:* 2,000 × 0.2 = **1,000 in flight** → pools (sized ~100) exhaust, requests queue, p99 explodes.

**Drill 18.** 1,000 jobs/sec arrive; each worker processes 50/sec. Workers needed (+headroom)?
*Solution:* 1,000/50 = 20; +headroom for spikes/retries → **~24 workers**.

**Drill 19.** Producers 30K msg/s, 8 Kafka partitions, each consumer 5K/s. Can consumers keep up?
*Solution:* Max 8 consumers (= partitions) × 5K = 40K/s capacity > 30K/s → **yes** (with headroom). If partitions were 4, max 20K < 30K → lag grows; add partitions.

**Drill 20.** A page fans out to 10 backend calls, each with 1% chance of being slow (p99). Chance the page hits ≥1 slow call?
*Solution:* 1 − 0.99¹⁰ ≈ **~9.6%** (tail amplification — a p99 backend → ~p90 page).

---

## Drills 21–23: Harder / combined

**Drill 21.** 1M DAU, 20 req/user/day, 5× peak, 90% reads. Peak read RPS and peak write RPS?
*Solution:* 20M/day ≈ 240 avg RPS; ×5 = 1,200 peak; reads 1,080, writes **~120** peak write RPS (modest — justifies no sharding; cache + maybe a replica).

**Drill 22.** 50M DAU chat, 40 msgs/day, 200B stored, 1yr, ×3. Peak write RPS (3×) and yearly storage?
*Solution:* 2B/day ≈ 23,000 avg write RPS; ×3 = **~70,000 peak write RPS**; storage 2B×200B×365×3 ≈ **~438 TB/year** → far exceeds one node → shard + write-optimized store (justifies wide-column).

**Drill 23.** Video platform: 1M uploads/day, avg 100MB original, transcode to 3 variants (total ~1.5× original), ×2 replication, 3-year retention. Storage?
*Solution:* originals 100 TB/day; +variants (1.5×) = 250 TB/day; ×2 = 500 TB/day; ×~1,000 days ≈ **~500 PB**. Object storage + aggressive tiering/retention essential (not a DB).

---

## How to use these in an interview

The goal of each drill is to produce an order-of-magnitude number that *justifies a design decision*:
- Modest RPS/writes (Drill 21) → keep it simple (no sharding).
- Huge write RPS/storage (Drills 22, 23) → shard / write-optimized store / object storage.
- Little's Law blowup (Drill 17) → timeouts + fix the DB, not more app servers.
- Cache math (Drills 13–15) → size to the working set.

Round, show the math, state the implication. See the specialized chapters for deeper treatment of each dimension.

---

## Revision summary

**Memorize:** 86,400 s/day; 1M/day ≈ 12 RPS; data-size powers of ten; Little's Law.
**Always:** round aggressively, apply peak × replication × retention, and tie the number to a decision.

**Rapid-fire**
1. 10M requests/day ≈ ? RPS → ~120.
2. 5,000 RPS × 30ms = ? in flight → 150.
3. 10M docs/day × 2MB × ×3 = ? /day → 60 TB/day.
4. 95% hit ratio cuts DB reads by? → 20×.
5. Max Kafka consumers that do work? → Partition count.

**Next:** [traffic-and-bandwidth-calculations](./traffic-and-bandwidth-calculations.md)
