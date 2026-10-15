# Estimation Drills

> Prerequisites: [00-foundations/back-of-the-envelope-estimation](../00-foundations/back-of-the-envelope-estimation.md)
> Related: [11-estimation-and-bottleneck-analysis/estimation-cheat-sheet](../11-estimation-and-bottleneck-analysis/estimation-cheat-sheet.md) (the 20+ drills live there)

This page points to the main drill set and adds timed-practice guidance. The 23 worked drills (traffic, storage, bandwidth, connections, queue, cache, latency) are in [estimation-cheat-sheet](../11-estimation-and-bottleneck-analysis/estimation-cheat-sheet.md).

---

## Where the drills are

The full progressive drill set (23 exercises with worked solutions) is in **[estimation-cheat-sheet](../11-estimation-and-bottleneck-analysis/estimation-cheat-sheet.md)**:
- Drills 1–5: traffic / RPS
- Drills 6–10: storage / retention
- Drills 11–15: bandwidth / cache
- Drills 16–20: connections / queue / latency / workers
- Drills 21–23: harder / combined

Don't re-read solutions — do them under a timer, then check.

---

## Timed practice protocol

1. **Memorize the anchors first** (from [estimation-cheat-sheet](../11-estimation-and-bottleneck-analysis/estimation-cheat-sheet.md)): 86,400 s/day; 1M/day ≈ 12 RPS; data-size powers of ten; Little's Law; ×replication, ×retention, ×peak-factor.
2. **Do each drill in under 2 minutes**, out loud, showing the math (as in an interview). Round aggressively.
3. **State the implication**, not just the number: "~1,200 peak RPS → modest → keep it simple, no sharding" or "~70K write RPS → shard + write-optimized store." The implication is the point.
4. **Check against the worked solution.** If your order of magnitude is off, re-check which multiplier you missed (usually peak factor, replication, or retention).

---

## The three estimation mistakes to drill out of yourself

1. **Users as RPS** — always convert users → actions/day → RPS → peak.
2. **Average, not peak** — multiply by a peak factor (state it: 2–10×).
3. **Forgetting ×replication / ×retention** on storage.

---

## Self-test: can you do these in <90 seconds each?

- 5M DAU, 30 actions/day, 4× peak → peak RPS? *(→ 150M/day ≈ 1,740 avg ×4 = ~7,000 peak.)*
- 20M messages/day, 300 bytes, 1yr, ×3 → yearly storage? *(→ 6 GB/day ×365 ×3 ≈ ~6.6 TB/year.)*
- 3,000 RPS × 15ms latency → in-flight concurrency? *(→ 45.)*
- 10,000 read RPS, 90% cache hit → DB read RPS? *(→ 1,000.)*
- Producers 25K/s, consumers 5K/s each, 6 partitions → keep up? *(→ max 6 × 5K = 30K > 25K ✓.)*

If any takes more than ~90 seconds or you fumble a multiplier, drill the cheat-sheet set until the anchors are automatic.

---

## Exercise

Pick 5 random drills from [estimation-cheat-sheet](../11-estimation-and-bottleneck-analysis/estimation-cheat-sheet.md), do them under a timer out loud, and for each state the design implication (simple vs shard vs cache). Log any missed in [PROGRESS](../PROGRESS.md) Part 3.

**Next:** [architecture-defense-questions](./architecture-defense-questions.md)
