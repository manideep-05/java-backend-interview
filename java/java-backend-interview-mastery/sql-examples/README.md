# sql-examples

Runnable SQL demonstrating the claims in chapters [15](../15-sql-and-relational-databases/), [16](../16-database-indexing-and-query-optimization/), and [17](../17-database-transactions-and-isolation/). Each script targets **PostgreSQL 16** by default and flags where **Oracle 19c** differs. Run them against a scratch database (e.g., a Postgres Testcontainer or a local instance) — they create and drop their own tables.

| Script | Chapter | Shows |
|---|---|---|
| `01_schema_and_seed.sql` | 15 | sample schema + seed data used by the other scripts |
| `02_joins_and_windows.sql` | 15 | LEFT-join WHERE trap, EXISTS anti-join, window functions, WHERE vs HAVING |
| `03_indexing_and_plans.sql` | 16 | composite index left-prefix, sargability, EXPLAIN ANALYZE |
| `04_isolation_and_lost_update.sql` | 17 | two-session lost update + four fixes, idempotent upsert, deadlock ordering |

> These are teaching scripts: short, commented, and safe to re-run. They are not a migration framework.
