# Storage and Retention Calculations

> Prerequisites: [estimation-cheat-sheet](./estimation-cheat-sheet.md)
> Related: [03-databases/data-archival-and-retention](../03-databases-and-data-modeling/data-archival-and-retention.md)

Projecting data growth over time, with the two multipliers people forget: replication and retention. These numbers decide SQL-vs-object-storage and shard-vs-single-node.

---

## A. Formulas

- **Daily data** = `writes/day × average record/object size`.
- **Over period** = `daily data × retention days`.
- **With replication** = `× replication factor` (×3 typical for durability).
- **Total** = `daily × retention × replication`.
- Separate **metadata** (small, in the DB) from **blobs** (large, in object storage) — they live in different stores and scale differently.

Growth over 1/3/5 years is a common ask — multiply daily by 365/1095/1825.

---

## B. Worked example — DMS (recap + extension)

500K docs/month uploaded; file 2MB, metadata 1KB; 5-year retention; ×3 replication.
- **Files:** 500K × 2MB = 1 TB/month → ×60 = 60 TB → ×3 = **~180 TB** → object storage (not a DB).
- **Metadata:** 500K × 1KB = 500 MB/month → ×60 = 30 GB → ×3 = **~90 GB** → single DB node for years (no sharding needed for metadata).

**The decision this drives:** files → object storage; metadata → one SQL node. Two different stores because the storage math is 2000× different. This is *why* the DMS architecture splits files from metadata.

---

## C. The multipliers people forget

- **Replication factor:** data is stored ×N for durability/availability (×3 typical) — raw size × replication.
- **Retention period:** data accumulates until deleted; multiply daily by the retention window. Without a retention policy, this grows unbounded (see [data-archival-and-retention](../03-databases-and-data-modeling/data-archival-and-retention.md)).
- **Indexes + overhead:** real DB storage is larger than raw data (indexes, WAL, fragmentation) — add a rough overhead factor (e.g., ×1.5–2) for DB storage.
- **Growth rate:** if the business is growing, model increasing daily volume, not a flat rate.

---

## D. Interview Q&A

- Q: "10M photos/day, 1.5MB, ×3, 2-year retention. Storage?" → *15 TB/day ×3 = 45 TB/day ×730 ≈ ~33 PB → object storage + tiering.*
- Q: "Your metadata is 90GB after 5 years — shard it?" → *No — 90GB fits one node comfortably; sharding adds complexity for no benefit. Shard only when size/write-rate approaches the node limit.*

**Common wrong answers:** quoting raw size (forgetting ×replication, ×retention); putting blobs in the DB; sharding small metadata; ignoring index overhead.

---

## E. Exercise

A logging platform ingests 1B log lines/day, 500 bytes each, kept 30 days hot (×2 replication) and 1 year cold (compressed ~10×, ×1 replication in object storage). Compute hot and cold storage. State which store each goes in.
*(Answer: raw 500 GB/day. Hot: 500GB × 30 × 2 = ~30 TB (hot store/TSDB). Cold: 500GB × 365 ÷ 10 (compression) × 1 ≈ ~18 TB compressed in object storage. Hot in a time-series/search store with short retention; cold compressed in object storage — see [data-archival-and-retention](../03-databases-and-data-modeling/data-archival-and-retention.md).)*

---

## F. Revision summary

**Key takeaways**
- Total storage = daily × retention × replication (+ index/overhead for DBs).
- Separate metadata (small, DB) from blobs (large, object storage) — the storage math often differs by orders of magnitude and dictates the store.
- Don't forget replication and retention multipliers; model growth; add DB overhead.
- Storage size drives SQL-vs-object-storage and shard-vs-single-node decisions.

**Rapid-fire**
1. Two multipliers people forget? → Replication factor, retention period.
2. 180 TB of files → which store? → Object storage.
3. 90 GB metadata → shard? → No (fits one node).
4. DB storage vs raw data? → Larger (indexes/WAL/overhead).
5. What makes storage grow unbounded? → No retention policy.

**Next:** [database-capacity-and-connection-pools](./database-capacity-and-connection-pools.md)
