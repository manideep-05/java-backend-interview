# 15 — SQL & Relational Databases

> **Priority: P0.** SQL is a major interview section for backend roles. The signal is writing non-trivial queries (joins, aggregation, window functions), reasoning about NULL semantics, and knowing portable vs dialect-specific behavior (Oracle vs PostgreSQL).

**Prerequisites:** basic SQL.
**Related:** [16 Indexing](../16-database-indexing-and-query-optimization/), [17 Transactions](../17-database-transactions-and-isolation/), [12 JPA](../12-spring-data-jpa-and-hibernate/).
SQL scripts: [`sql-examples/`](../sql-examples/).

---

## A. Concept & intuition
A relational database stores data as tables (relations) with typed columns and constraints, and SQL is the declarative language to query/modify it: you say *what* you want, the optimizer decides *how*. Constraints (PK, FK, UNIQUE, CHECK, NOT NULL) push correctness into the DB so bad data can't exist regardless of application bugs — a key reason to use the DB's guarantees instead of app-only checks ([17 §idempotency](../17-database-transactions-and-isolation/)).

## B. Core concepts

### Joins
- **INNER**: rows matching on both sides.
- **LEFT**: all left rows + matched right (NULLs where no match).
- **RIGHT/FULL**: symmetric / both sides.
- **SELF**: a table joined to itself (hierarchies, pairs).
A common trap: filtering a LEFT join's right-side column in `WHERE` turns it into an inner join (NULLs filtered out); put the condition in the `ON` clause instead.

### Subqueries, correlated subqueries, CTEs
- Subquery: a query nested in another.
- Correlated: references the outer row (runs per outer row conceptually).
- **CTE** (`WITH`): names a subquery for readability; recursive CTEs traverse hierarchies.

### Aggregation; WHERE vs HAVING
`GROUP BY` collapses rows; aggregates (`COUNT`, `SUM`, `AVG`, `MIN`, `MAX`) summarize groups. **`WHERE` filters rows before grouping; `HAVING` filters groups after aggregation.**

### NULL and three-valued logic
NULL means "unknown," not zero/empty. Comparisons with NULL yield **UNKNOWN**, not true — so `x = NULL` is never true (use `IS NULL`); `NOT IN (subquery with a NULL)` can return no rows unexpectedly; aggregates skip NULLs (except `COUNT(*)`). This is a top trap.

### EXISTS vs IN
`EXISTS` stops at the first match and handles NULLs cleanly; `IN` with a subquery that can produce NULL has the `NOT IN` pitfall. Prefer `EXISTS`/`NOT EXISTS` for correlated existence checks.

### UNION vs UNION ALL
`UNION` removes duplicates (adds a sort/hash — cost); `UNION ALL` keeps them (cheaper). Use `UNION ALL` unless you truly need dedup.

### Window functions
Compute across a set of rows *related to the current row* without collapsing them: `ROW_NUMBER()`, `RANK()`, `DENSE_RANK()`, running totals (`SUM() OVER (ORDER BY ...)`), `LAG`/`LEAD`. Essential for "top-N per group," running totals, deduplication.

### Pagination
`LIMIT/OFFSET` (Postgres) / `FETCH FIRST n ROWS` (Oracle 12c+, also standard) is simple but slow for deep pages (the DB scans and discards `OFFSET` rows). **Keyset pagination** (`WHERE (sort_col, id) > (:last_sort, :last_id) ORDER BY sort_col, id LIMIT n`) is efficient and stable ([16](../16-database-indexing-and-query-optimization/)).

### Upserts & sequences
- Postgres: `INSERT … ON CONFLICT (key) DO UPDATE/NOTHING`; Oracle: `MERGE`.
- Identity: Postgres `GENERATED ALWAYS AS IDENTITY` / `SERIAL`; Oracle sequences / identity columns.

### Normalization
Normalize to remove redundancy/update anomalies (1NF→3NF); denormalize deliberately for read performance, accepting duplication you must keep consistent.

## C. Code — interview-style queries
```sql
-- Second-highest salary per department (window function)
SELECT department_id, employee_id, salary
FROM (
  SELECT department_id, employee_id, salary,
         DENSE_RANK() OVER (PARTITION BY department_id ORDER BY salary DESC) AS rnk
  FROM employee
) t
WHERE rnk = 2;
```
```sql
-- Customers with no orders (anti-join) — NOT EXISTS avoids the NOT IN/NULL trap
SELECT c.id, c.name
FROM customer c
WHERE NOT EXISTS (SELECT 1 FROM orders o WHERE o.customer_id = c.id);
```
```sql
-- Running total of daily revenue
SELECT day, revenue,
       SUM(revenue) OVER (ORDER BY day ROWS UNBOUNDED PRECEDING) AS running_total
FROM daily_sales
ORDER BY day;
```
```sql
-- WHERE vs HAVING: departments with >5 employees earning over 100k
SELECT department_id, COUNT(*) AS cnt
FROM employee
WHERE salary > 100000        -- row filter (before grouping)
GROUP BY department_id
HAVING COUNT(*) > 5;         -- group filter (after aggregation)
```
```sql
-- The LEFT JOIN trap
-- WRONG: becomes an inner join (NULL o.id filtered out)
SELECT c.*, o.id FROM customer c LEFT JOIN orders o ON o.customer_id=c.id WHERE o.status='PAID';
-- RIGHT: keep the outer rows, move the condition into ON
SELECT c.*, o.id FROM customer c LEFT JOIN orders o ON o.customer_id=c.id AND o.status='PAID';
```

### Dialect notes
| Feature | PostgreSQL | Oracle |
|---|---|---|
| Pagination | `LIMIT n OFFSET m` | `OFFSET m ROWS FETCH NEXT n ROWS ONLY` |
| Upsert | `INSERT … ON CONFLICT` | `MERGE` |
| String concat | `||` or `concat` | `||` or `concat` |
| Empty string | distinct from NULL | **`''` is treated as NULL** (classic Oracle gotcha) |
| Auto id | IDENTITY/SERIAL | sequence / IDENTITY |
| Boolean type | native `boolean` | no native boolean (use NUMBER(1)/CHAR) |

## D. Trade-offs

| Choice | Option A | Option B | Guidance |
|---|---|---|---|
| Existence check | `IN` | `EXISTS` | EXISTS for correlated/NULL-safe |
| Dedup union | `UNION` | `UNION ALL` | ALL unless dedup needed |
| Deep pagination | OFFSET | keyset | keyset for deep/stable paging |
| Shape | normalized | denormalized | normalize by default; denormalize for measured read wins |
| Logic location | triggers/procs | app code | app code usually; procs for data-local bulk/consistency |

## E. Interview Q&A

### SQL-01 (P1) — INNER vs LEFT join, and the common WHERE trap?
**Strong answer:** INNER returns only matched rows; LEFT returns all left rows with NULLs where the right has no match. The trap is putting a right-table predicate in `WHERE` on a LEFT join — NULL rows fail the predicate and get filtered, silently making it an inner join. The fix is to move the condition into the `ON` clause so unmatched left rows survive.

### SQL-02 (P1) — WHERE vs HAVING?
**Strong answer:** `WHERE` filters individual rows before grouping and can't use aggregates; `HAVING` filters groups after aggregation and can. For performance I filter as much as possible in `WHERE` so fewer rows reach the grouping step.

### SQL-03 (P1) — Explain NULL and three-valued logic.
**Strong answer:** NULL means unknown, so comparisons yield UNKNOWN, not true — `x = NULL` is never true (use `IS NULL`), aggregates skip NULLs except `COUNT(*)`, and `NOT IN` against a subquery containing a NULL returns no rows because the comparison can't be proven true. I use `IS [NOT] NULL`, `COALESCE`, and prefer `NOT EXISTS` over `NOT IN` to stay NULL-safe.

<a id="sql-04"></a>
### SQL-04 (P0) — EXISTS vs IN — semantics and performance?
**Strong answer:** Both test membership, but `EXISTS` short-circuits on the first match and is NULL-safe, while `NOT IN` breaks if the subquery yields a NULL (returns no rows). Performance depends on the optimizer and data, but `EXISTS`/`NOT EXISTS` are the safe default for correlated existence/anti-join checks; `IN` is fine for small constant lists. I verify with the execution plan rather than assuming one is always faster.

### SQL-05 (P1) — UNION vs UNION ALL?
**Strong answer:** `UNION` removes duplicates, which forces a sort or hash and costs more; `UNION ALL` concatenates without dedup and is cheaper. I use `UNION ALL` unless duplicates are actually possible and undesired.

### SQL-06 (P1) — When do you denormalize?
**Strong answer:** Normalization (to 3NF) removes redundancy and update anomalies and is my default. I denormalize deliberately — duplicating or precomputing data — only when a measured read pattern needs it (e.g., a hot aggregate), accepting that I now must keep the duplicate consistent on writes.

<a id="sql-07"></a>
### SQL-07 (P0) — Solve a top-N-per-group / running-total problem with window functions.
**Strong answer:** Window functions compute over a partition without collapsing rows. For top-N per group I use `DENSE_RANK() OVER (PARTITION BY group ORDER BY metric DESC)` and filter the rank; for running totals `SUM(x) OVER (ORDER BY t ROWS UNBOUNDED PRECEDING)`; for dedup, `ROW_NUMBER()` partitioned by the dedup key and keep rank 1. The key distinction from `GROUP BY` is that windows preserve individual rows while adding the computed column.

### SQL-08 (P1) — OFFSET vs keyset pagination?
**Strong answer:** `LIMIT/OFFSET` is simple but the DB must scan and discard `OFFSET` rows, so deep pages get slow and results can shift if rows are inserted. Keyset pagination remembers the last row's sort key and does `WHERE (sort, id) > (:last_sort, :last_id) ORDER BY sort, id LIMIT n`, which uses an index and stays stable — I use it for infinite scroll and deep paging.

### SQL-09 (P2) — Views vs materialized views vs stored procedures?
**Strong answer:** A view is a saved query (no stored data, always fresh, can be slow). A materialized view stores the result and must be refreshed (fast reads, possibly stale). Stored procedures run logic in the DB — useful for data-local bulk operations and enforcing consistency, but they move business logic out of the app, are harder to test/version, and tie you to the DB; I use them sparingly.

## F. Production relevance
SQL fluency decides whether features are fast and correct: the LEFT-join `WHERE` trap silently drops data; NULL/three-valued mistakes produce wrong counts and empty `NOT IN` results; OFFSET pagination melts under deep pages; window functions replace slow app-side loops; and constraints (UNIQUE/FK/CHECK) are your last line of defense against bad data and duplicate records ([17](../17-database-transactions-and-isolation/)). Knowing the Oracle-vs-Postgres gotchas (empty string = NULL in Oracle, no native boolean) prevents migration bugs.

## G. Revision

### 10 key takeaways
1. Declarative: you say what, optimizer decides how; constraints enforce correctness.
2. LEFT join + right-column `WHERE` = accidental inner join; use `ON`.
3. `WHERE` before grouping, `HAVING` after.
4. NULL = unknown; `IS NULL`, NULL-safe `NOT EXISTS`.
5. EXISTS short-circuits and is NULL-safe; beware `NOT IN` + NULL.
6. `UNION ALL` unless you need dedup.
7. Window functions = per-row computations over partitions (top-N, running totals, dedup).
8. Keyset pagination beats OFFSET for deep pages.
9. Upsert: `ON CONFLICT` (PG) / `MERGE` (Oracle).
10. Oracle: `''` is NULL, no native boolean; different pagination syntax.

### 10 rapid-fire
1. Filter groups? (HAVING) 2. `x = NULL`? (never true) 3. Dedup union? (UNION) 4. Top-N per group? (RANK/DENSE_RANK window) 5. Deep paging? (keyset) 6. Anti-join safely? (NOT EXISTS) 7. PG upsert? (ON CONFLICT) 8. Oracle upsert? (MERGE) 9. Count including nulls? (COUNT(*)) 10. Oracle empty string? (NULL)

### 5 tricky
1. Why did a LEFT join lose rows? (WHERE on right col) 2. Why did `NOT IN` return nothing? (NULL in subquery) 3. Why is deep OFFSET slow? 4. GROUP BY vs window for top-N? 5. Why count mismatch with NULLs?

### 5 exercises
1. Write second-highest-salary-per-dept with a window function.
2. Find customers with no orders using `NOT EXISTS`.
3. Convert OFFSET pagination to keyset.
4. Reproduce the LEFT-join WHERE trap and fix it.
5. Write an idempotent upsert in both Postgres and Oracle syntax.

### One-page revision sheet
> Declarative + constraints. **LEFT join: right-col predicate goes in ON, not WHERE** (else inner join). WHERE(rows, pre-group) vs HAVING(groups, post-agg). **NULL=unknown**: IS NULL, NOT EXISTS (not NOT IN). EXISTS short-circuits/NULL-safe. UNION ALL default. **Window funcs** (ROW_NUMBER/RANK/DENSE_RANK, SUM OVER) = per-row over partition: top-N, running total, dedup. **Keyset > OFFSET** deep paging. Upsert ON CONFLICT(PG)/MERGE(Oracle). Oracle: ''=NULL, no boolean, OFFSET…FETCH.

### Prerequisites to revisit
- None hard; pairs with indexing (ch.16) and transactions (ch.17); drives JPA queries (ch.12).
