# Backups and Point-in-Time Recovery

> Prerequisites: [disaster-recovery](./disaster-recovery.md), [03-databases/database-internals-for-system-design](../03-databases-and-data-modeling/database-internals-for-system-design.md)
> Related: [data-archival-and-retention](../03-databases-and-data-modeling/data-archival-and-retention.md)

Backups are the last line of defense against data loss from logical errors (bad deploys, deletions, corruption) that replication can't protect against. The key SDE-2 points: backups ≠ replicas, point-in-time recovery via the WAL, and "an untested backup doesn't exist."

---

## A. Problem and motivation

Replication (replicas) protects against *node* failure, but faithfully copies *logical* errors — a bad migration, an accidental `DELETE FROM users`, application-level corruption — to every replica. Backups are a *separate, point-in-time* copy you can restore from to undo such errors. Without good, tested backups, one bad command or deploy can be unrecoverable. This is one of the most consequential and most-neglected reliability topics.

---

## B. Backup types

| Type | What | Pros | Cons |
|---|---|---|---|
| **Full** | Complete copy of the dataset | Simple restore | Large, slow, storage-heavy |
| **Incremental** | Only changes since the last backup | Small, fast | Restore needs the full + all increments in order |
| **Snapshot** | Point-in-time image (often storage-level) | Fast to take | Depends on storage; may not be app-consistent |
| **Continuous (WAL archiving)** | Stream the WAL continuously | Enables PITR to any moment | More to manage |

Most production DBs combine a periodic **full/snapshot** base with **continuous WAL archiving** to enable restoring to *any point in time* (not just the last backup).

---

## C. Point-in-time recovery (PITR) via the WAL

The [WAL](../03-databases-and-data-modeling/database-internals-for-system-design.md) (every committed change, in order) is the key. PITR works by: restore the base backup, then **replay the WAL up to a chosen moment** (e.g., "just before the bad deploy at 14:32"). This lets you recover to the instant *before* a corruption/deletion — far more precise than restoring the last nightly backup (which would lose a day of good data too).

```mermaid
flowchart LR
    Base[Base backup, e.g., midnight] --> Replay[Replay WAL]
    Replay -->|stop at 14:31:59| Target[Restored to just before the 14:32 bad deploy]
```

This is the same WAL mechanism that powers replication and CDC — one log, many uses.

---

## D. The cardinal rule: test restores

> An untested backup is not a backup — it's a hope.

Common disasters: backups that silently stopped running months ago; backup files that are corrupt/incomplete; restore procedures that don't work or take far longer than the RTO; backups of some tables but not others. The only way to know your backups work is to **regularly restore them** to a scratch environment and verify integrity — and to measure restore *time* against your RTO (see [disaster-recovery](./disaster-recovery.md)). Automate restore tests.

**Also:** store backups **separately** (different region/account) so the disaster that hits production doesn't also destroy the backups, and encrypt them (they contain all your data — see [encryption-and-data-protection](../08-security/encryption-and-data-protection.md)).

---

## E. Example — DMS

- **Metadata DB:** nightly full snapshot + continuous WAL archiving to a different region → PITR to any minute. If a bad deploy corrupts data at 14:32, restore to 14:31 (losing ~minutes, not a day). Restore tests run weekly to a scratch instance, verifying integrity and measuring restore time vs the 1-hour RTO.
- **Files (object storage):** versioning enabled (so overwrites/deletes are recoverable) + cross-region replication; object storage's own durability is very high, but versioning protects against logical deletes.
- Backups encrypted, stored cross-region, retention per policy (see [data-archival-and-retention](../03-databases-and-data-modeling/data-archival-and-retention.md)).

---

## F. Trade-offs / failure scenarios

| Scenario | Without good backups | With PITR + tested restores |
|---|---|---|
| Accidental `DELETE`/`DROP` | Gone (replicas replicated it) | Restore to just before |
| Bad deploy corrupts data | Corruption everywhere | PITR to pre-deploy |
| Backups never tested | Restore fails during the disaster | Verified in advance |
| Backups co-located with prod | Disaster destroys backups too | Separate region/account |

Failure modes: silent backup failure (monitor backup success!), untested restores, co-located backups, slow restores blowing the RTO, unencrypted backups (a data-breach vector).

---

## G. Scaling implications

As data grows, full backups become slow and storage-heavy, and restore time (part of RTO) grows — pushing toward incremental/continuous backups and toward retention/archival to keep the *hot* dataset (and thus backup/restore time) manageable (see [data-archival-and-retention](../03-databases-and-data-modeling/data-archival-and-retention.md)). PITR scales well because WAL archiving is continuous and incremental. At scale, restore-time testing matters more (a backup that takes 12 hours to restore fails a 1-hour RTO regardless of integrity).

---

## H. Interview preparation

**Basic**
- Q: Why aren't replicas enough — why also back up? → *A: Replicas copy logical errors (bad deletes/corruption) to all replicas; backups are a separate point-in-time copy you can restore from to undo them.*

**Intermediate**
- Q: What is point-in-time recovery? → *A: Restore a base backup, then replay the WAL up to a chosen moment — recovering to the instant before a corruption/deletion, not just the last backup.*
- Q: Why test restores? → *A: Backups silently fail, corrupt, or restore too slowly; you only know they work by regularly restoring them and measuring restore time against the RTO.*

**SDE-2 / scenario**
- Q: "A migration corrupted data an hour ago. How do you recover?" → *A: Point-in-time recovery: restore the base backup and replay the WAL to just before the migration, losing only the minutes between. This needs continuous WAL archiving — and the restore procedure must have been tested.*
- Q: "Where do you store backups and why?" → *A: In a separate region/account (so the disaster hitting prod doesn't destroy backups) and encrypted (they contain all the data). Monitor that backups actually run.*

**Common wrong answers:** replicas as backups; nightly-full-only (coarse recovery, lose a day); untested backups; backups co-located with production; unencrypted backups.

---

## I. Hands-on exercise

Design the backup/PITR strategy for a DB to meet RPO=5min, RTO=30min. Specify backup types + cadence, how PITR works, where backups are stored, your restore-testing plan, and how you'd recover from an accidental table drop 10 minutes ago.

---

## J. Revision summary

**Key takeaways**
- Backups protect against logical errors (deletes/corruption/bad deploys); replicas don't (they copy them).
- PITR = base backup + WAL replay to any moment → recover to just before the error.
- Test restores regularly (and measure restore time vs RTO) — an untested backup isn't a backup.
- Store backups separately (region/account) and encrypted; monitor that they run.

**Rapid-fire**
1. Do replicas substitute for backups? → No.
2. How does PITR achieve any-moment recovery? → WAL replay on top of a base backup.
3. The cardinal backup rule? → Test restores.
4. Where to store backups? → Separate region/account, encrypted.
5. Backup type enabling PITR? → Continuous WAL archiving.

**Next:** [idempotent-recovery-and-reconciliation](./idempotent-recovery-and-reconciliation.md)
