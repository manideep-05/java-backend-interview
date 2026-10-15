# Disaster Recovery, RPO, and RTO

> Prerequisites: [redundancy-and-failover](./redundancy-and-failover.md), [backups-and-point-in-time-recovery](./backups-and-point-in-time-recovery.md)
> Related: [06-distributed-systems/multi-region-architecture](../06-distributed-systems/multi-region-architecture.md) · [slis-slos-and-slas](./slis-slos-and-slas.md)

Disaster recovery (DR) is how you survive catastrophic failures — a whole region down, data corruption, accidental mass deletion. The two numbers that define a DR strategy: **RPO** (how much data you can lose) and **RTO** (how long recovery takes).

---

## A. Problem and motivation

Redundancy and failover handle component failures. DR handles *catastrophes*: an entire datacenter/region lost, a bad deploy corrupting data, an operator dropping a production table. These aren't single-component failures — they can take out your redundant components too. DR defines, in advance, how fast and how completely you recover, so a catastrophe is a bounded, rehearsed event rather than an existential one.

---

## B. The two defining numbers

```mermaid
flowchart LR
    L[Last backup/replication point] -->|data since here is lost = RPO| D[Disaster]
    D -->|time to restore service = RTO| R[Recovered]
```

- **RPO (Recovery Point Objective):** the maximum acceptable **data loss**, measured in time. "RPO = 5 minutes" means you can lose at most the last 5 minutes of data. Driven by backup frequency / replication lag.
- **RTO (Recovery Time Objective):** the maximum acceptable **downtime** to recover. "RTO = 1 hour" means service must be back within an hour. Driven by how fast you can restore/fail over.

These are *business decisions* (how much loss/downtime is tolerable), and they drive the DR architecture and its cost. Tighter RPO/RTO = more expensive.

---

## C. DR strategies (by RPO/RTO and cost)

| Strategy | RPO | RTO | Cost | How |
|---|---|---|---|---|
| **Backup & restore** | Hours (last backup) | Hours (restore + rebuild) | Low | Periodic backups to another region; restore on disaster |
| **Pilot light** | Minutes | Tens of min | Medium | Core systems replicated + minimal standby, scaled up on disaster |
| **Warm standby** | Seconds–minutes | Minutes | Higher | Scaled-down full replica running; scale up + redirect on disaster |
| **Active-active (multi-region)** | ~Zero | ~Zero | Highest | Both regions serve; failure is seamless (see [multi-region](../06-distributed-systems/multi-region-architecture.md)) |

Choose based on the required RPO/RTO vs cost. Most systems don't need active-active DR; a warm standby or even backup-and-restore meets their RPO/RTO far more cheaply.

---

## D. The critical rule: test your DR

An untested backup/DR plan is a hope, not a plan. Backups that can't be restored, failover scripts that don't work, and runbooks nobody has rehearsed are discovered *during* the disaster — the worst time. **Regularly test:** restore backups to verify they work, run failover drills (game days / chaos exercises), and measure actual RPO/RTO against targets. See [backups-and-point-in-time-recovery](./backups-and-point-in-time-recovery.md).

---

## E. Example — DMS DR plan

- **RPO = 5 min, RTO = 1 hour** (a document system can tolerate this; it's not a payment ledger).
- **Metadata DB:** continuous backups + cross-region replica; on region loss, promote the cross-region replica (data loss bounded by replication lag ≈ RPO). Point-in-time recovery handles corruption/bad-deploy cases (restore to just before).
- **Files (object storage):** cross-region replication (object storage durability is very high; cross-region copy for region loss).
- **DR drills quarterly:** restore a backup to a scratch environment and verify; run a failover game day; measure actual RPO/RTO.
- Not active-active (RTO of 1 hour doesn't justify the cost).

---

## F. Trade-offs / failure scenarios

| Scenario | Without DR | With DR |
|---|---|---|
| Region lost | Total, possibly permanent outage/data loss | Fail over within RTO, lose ≤ RPO |
| Data corruption / bad deploy | Corrupt data everywhere (replicas too) | Point-in-time restore to before the corruption |
| Accidental mass deletion | Gone (replicas replicate the delete!) | Restore from backup (replicas don't help here) |

Failure modes: **untested backups** (can't restore when it matters); **replicas mistaken for backups** (replication copies a bad delete/corruption to all replicas — backups are a *separate*, point-in-time copy); **RPO/RTO undefined** (no target → chaotic recovery). The replica-vs-backup distinction is a key interview point: replicas protect against node failure, backups protect against logical errors (deletion/corruption).

---

## G. Scaling implications

DR cost scales with how tight your RPO/RTO must be — most systems should pick the loosest acceptable targets to control cost (backup-and-restore or warm standby), reserving active-active for genuine zero-downtime requirements. As data grows, backup/restore *time* grows (affecting RTO), pushing toward incremental backups and retention/archival (see [data-archival-and-retention](../03-databases-and-data-modeling/data-archival-and-retention.md)) to keep the hot set — and thus restore time — manageable. Multi-region DR ties directly to [multi-region-architecture](../06-distributed-systems/multi-region-architecture.md).

---

## H. Interview preparation

**Basic**
- Q: RPO vs RTO? → *A: RPO = max acceptable data loss (in time); RTO = max acceptable downtime to recover. Both are business decisions driving the DR architecture.*

**Intermediate**
- Q: Why aren't replicas a backup? → *A: Replicas copy *everything*, including a bad delete or corruption — so they protect against node failure, not logical errors. A backup is a separate point-in-time copy you can restore from.*
- Q: Why test DR? → *A: Untested backups/failover often don't work; you discover it during the disaster. Regular restore tests and failover drills verify RPO/RTO are actually met.*

**SDE-2 / scenario**
- Q: "An operator dropped the production table. Replicas have the drop too. Now what?" → *A: Replicas don't help (they replicated the drop). Restore from a backup / point-in-time recovery to just before the drop — which is why backups (separate from replication) and PITR exist, and why they must be tested.*
- Q: "Design DR for your system." → *A: Set RPO/RTO from business tolerance; choose a strategy to match (backup-and-restore / warm standby / active-active) at the lowest cost that meets them; cross-region backups + replica for region loss; PITR for corruption; and quarterly DR drills measuring actual RPO/RTO.*

**Common wrong answers:** replicas as backups; untested backups; undefined RPO/RTO; active-active DR when unneeded.

---

## I. Hands-on exercise

Define a DR plan for an e-commerce order DB with RPO=1min, RTO=15min. Specify the strategy, how you meet each target, how you'd recover from (a) a region outage and (b) a corrupting bad deploy, and your DR testing cadence. Note why the corruption case needs backups, not replicas.

---

## J. Revision summary

**Key takeaways**
- RPO = max data loss (time); RTO = max downtime; both are business-driven and set the DR architecture + cost.
- Strategies by cost/tightness: backup-and-restore → pilot light → warm standby → active-active.
- Replicas ≠ backups: replicas copy corruption/deletes; backups + PITR are the recovery for logical errors.
- Test DR regularly (restore tests, failover drills) — an untested plan isn't a plan.

**Rapid-fire**
1. RPO measures? → Max data loss (in time).
2. RTO measures? → Max downtime to recover.
3. Do replicas protect against an accidental DROP? → No (they replicate it) — need backups.
4. Cheapest DR strategy? → Backup-and-restore.
5. Most important DR practice? → Testing (restore + failover drills).

**Next:** [backups-and-point-in-time-recovery](./backups-and-point-in-time-recovery.md)
