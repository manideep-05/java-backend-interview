# Connection Pool Configuration (Java / Spring Boot + HikariCP)

> Concept: [database-capacity-and-connection-pools](../11-estimation-and-bottleneck-analysis/database-capacity-and-connection-pools.md) · [horizontal-vs-vertical-scaling](../02-compute-and-architecture/horizontal-vs-vertical-scaling.md)

**HLD point:** DB connections are scarce (bounded by `max_connections`); pools should be **small**, sized from Little's Law — not large defaults. Scaling app instances multiplies connections to the single DB (the recurring journey trap).

**Where the concern lives:** app (HikariCP config) + awareness of the DB's connection ceiling.

```yaml
# application.yml — HikariCP (Spring Boot's default pool)
spring:
  datasource:
    hikari:
      maximum-pool-size: 10        # per instance — SMALL, sized from Little's Law (see below)
      minimum-idle: 2
      connection-timeout: 2000     # ms to wait for a connection before failing fast (don't hang)
      max-lifetime: 1800000        # recycle connections (avoid stale)
      validation-timeout: 1000
      leak-detection-threshold: 20000  # log connections held too long (find leaks)
```

**Sizing it (Little's Law):**
```
connections needed = DB-op RPS x DB-op latency
e.g. 1,000 read RPS x 10ms = 10 concurrent DB ops  -> pool ~= 10 (+ small headroom), NOT 50
```
And across instances:
```
total DB connections = per-instance pool x number of app instances
e.g. 10 x 15 instances = 150  -> must stay under the DB's max_connections (often a few hundred)
```

```java
// connection-timeout makes the app FAIL FAST when the pool is exhausted
// (instead of hanging and cascading). Pair with a circuit breaker / retry.
try {
    jdbcTemplate.query(...);
} catch (CannotGetJdbcConnectionException e) {   // pool exhausted / timeout
    throw new ServiceUnavailableException("db busy"); // 503, don't hang
}
```

**Key points to say:**
- Pools should be **small** — sized from `RPS × latency` (Little's Law), not a big default. More connections past the optimum *reduce* DB throughput (context-switching) and risk exhaustion.
- **Scaling app instances multiplies DB connections** (instances × pool) — the classic trap where "add more app servers" overwhelms the single DB. Cap the total under `max_connections`; add a **connection pooler** (PgBouncer-style) to multiplex many app connections onto few DB connections at scale (and for serverless fan-out).
- Set a **connection-timeout** so a saturated pool fails fast (not hangs → cascade).
- `leak-detection-threshold` surfaces connections held too long (unclosed/long transactions).

**Caveat:** production needs the pool sized against real measured latency/RPS, a connection pooler at scale, and read/write split (replica datasource) when reads dominate.

**Maps to your resume:** your 90% Oracle optimization — right-sizing pools and avoiding long transactions complements the index/query fixes; and your JobFinder (Spring + PostgreSQL) uses HikariCP by default.
