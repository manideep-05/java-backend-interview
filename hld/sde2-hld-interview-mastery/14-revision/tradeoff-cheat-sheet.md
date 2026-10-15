# Trade-off Cheat Sheet

> Condensed from [tradeoff-questions](../13-interview-practice/tradeoff-questions.md). Pattern: gains X, costs Y, use when Z. No universal "best."

| Decision | Option A (gain / cost) | Option B (gain / cost) | A when / B when |
|---|---|---|---|
| **SQL vs NoSQL** | SQL: queries+ACID / hard write-scale | NoSQL: horizontal scale / limited queries+txns | SQL default; NoSQL for a known access pattern at scale |
| **Strong vs eventual consistency** | Strong: correct / latency+less available | Eventual: fast+available / stale reads | Strong for money/inventory; eventual for feeds/counts |
| **Normalize vs denormalize** | Normalized: consistent+simple writes / join-heavy reads | Denormalized: fast reads / write amplification+consistency | Normalize default; denormalize a measured read hot path |
| **Sync vs async** | Sync: simple+immediate / couples latency, cascades | Async: fast+decoupled+spike-absorbing / eventual+duplicates+ordering | Sync for needed-now; async for slow/heavy/unreliable |
| **Push vs pull (feed)** | Push: fast reads / celebrity write amplification | Pull: cheap writes / expensive reads | Hybrid: push normal, pull celebrities |
| **Monolith vs microservices** | Monolith: simple+local txns / scales as a unit | Micro: independent deploy/scale/own / network+distributed-txns+ops | Modular monolith default; decompose on team/scaling pressure |
| **Horizontal vs vertical** | Horizontal: unbounded+redundant / needs stateless, doesn't fix DB | Vertical: simple / ceiling+SPOF | App tier horizontal; DB vertical→replicas→shard |
| **Cache-aside vs write-through** | Aside: memory-efficient+resilient / lazy | Write-through: consistent-on-write+warm / per-write latency | Aside default; write-through for read-after-write |
| **Write-around vs write-behind** | Around: clean cache / read-after-write miss | Behind: fast writes+low DB load / data loss on crash | Around for write-heavy-rarely-read; behind for loss-tolerant counters |
| **Optimistic vs pessimistic lock** | Optimistic: no locks+scales / retries on conflict | Pessimistic: safe under contention / deadlocks | Optimistic for rare conflicts; pessimistic for hot rows |
| **Leader vs leaderless replication** | Leader: no conflicts+simple / write-scale limited | Leaderless(quorum): write-scale+available / conflict resolution | Leader default; leaderless for write-anywhere HA |
| **At-least-once vs at-most-once** | At-least: never loses / duplicates (need idempotency) | At-most: no duplicates / may lose | At-least+idempotency for business; at-most for loss-tolerant |
| **2PC vs saga** | 2PC: atomic / blocking+SPOF+unavailable | Saga: available+scalable / eventual+compensations | Avoid 2PC across services; saga for cross-service workflows |
| **CDN vs origin** | CDN: low latency+offload / invalidation+cost | Origin: dynamic/private / full load | CDN for static/cacheable; origin for dynamic/private |
| **Rolling vs blue-green vs canary** | Rolling: no downtime, no extra infra / both versions run | Blue-green: instant switch/rollback / 2× infra · Canary: smallest blast radius / slower+needs metrics | Rolling routine; canary risky; blue-green for instant cutover |
| **B-tree vs LSM** | B-tree: fast reads+ranges / moderate writes | LSM: fast writes / read amplification+compaction | B-tree for read/ad-hoc; LSM for write-heavy |
| **Local vs distributed cache** | Local(Caffeine): fastest / per-instance+cold-on-restart | Distributed(Redis): shared+consistent / network hop+dependency | Local for tiny change-tolerant; distributed for shared; layer both |
| **Offset vs cursor pagination** | Offset: jump-to-page / slow deep+unstable | Cursor: constant+stable / sequential only | Cursor for large/feeds; offset for small/admin |
| **JWT vs session** | JWT: stateless+scales / hard revoke | Session: instant revoke / shared-store lookup | JWT+short TTL for scale; session for tight control |
| **RBAC vs ABAC vs ReBAC** | RBAC: simple / coarse | ABAC: contextual / complex · ReBAC: relationship/hierarchy / specialized | RBAC default; ABAC for context rules; ReBAC for nested sharing |
| **Active-passive vs active-active** | Passive: simple+no conflicts / idle+failover delay | Active-active: low latency+seamless / write conflicts+complex | Passive for DR; active-active for global low-latency HA |

**Golden rule:** never say "X is better" — say "X gains ___ at the cost of ___; use it when ___." Every real decision is a trade-off.

---
**Next:** [technology-selection-matrix](./technology-selection-matrix.md)
