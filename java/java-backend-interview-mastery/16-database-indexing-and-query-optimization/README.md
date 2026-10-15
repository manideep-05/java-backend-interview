# 16 — Database Indexing & Query Optimization

> **Priority: P0.** "Why is a correct-looking composite index ignored?" and "how do you tell an index will help?" are classic SDE-2 questions. The signal is reasoning from an execution plan with evidence, not adding indexes by reflex.

**Prerequisites:** [15 SQL](../15-sql-and-relational-databases/).
**Related:** [12 JPA N+1](../12-spring-data-jpa-and-hibernate/), [17 Transactions](../17-database-transactions-and-isolation/), [22 Debugging](../22-debugging-and-production-engineering/).

---

## A. Concept & intuition
An index is a secondary data structure (usually a **B-tree**) that lets the DB find rows by key without scanning the whole table — like a book's index. It trades write cost and storage for read speed. The core optimization skill is knowing *when* an index helps, *why* the optimizer might ignore it, and *verifying* with the execution plan rather than guessing.

## B. Internals

### B-tree indexes
A balanced tree sorted by the key; lookups, range scans, and ordered reads are O(log n) to find the start then sequential. Because it's **sorted**, a B-tree accelerates equality, range (`>`, `BETWEEN`), prefix `LIKE 'abc%'`, `ORDER BY`, and `MIN/MAX`. It does **not** help a leading-wildcard `LIKE '%abc'` (no sorted prefix) or a function applied to the column (see sargability).

### Composite indexes & column order (the big one)
An index on `(a, b, c)` is sorted by `a`, then `b`, then `c` — like a phone book sorted by last then first name. It can serve predicates that use a **left prefix**: `a`, `a,b`, `a,b,c`. It generally **cannot** be used efficiently for a query filtering only on `b` or `c` (no leading `a`), just as you can't find someone by first name in a book sorted by last name. **This is why a "correct-looking" composite index gets ignored** — the query's predicates don't match the leading columns. Put the most selective / most-filtered column first, and order to match your common queries' equality-then-range pattern (equality columns before the range column).

### Covering index
If the index contains all columns a query needs (key + `INCLUDE`d columns), the DB answers from the index alone — an **index-only scan**, no table lookup. Great for hot read paths.

### Selectivity & cardinality
Selectivity = fraction of rows a predicate keeps. An index on a low-cardinality column (e.g., a boolean/status with few distinct values) is often **ignored** because scanning the table is cheaper than many random index lookups. The optimizer uses **statistics** (row counts, histograms) to estimate; **stale statistics** cause bad plans — refresh with `ANALYZE` (Postgres) / `DBMS_STATS` (Oracle).

### Sargability
A predicate is **sargable** (Search ARGument-able) if an index can be used. Non-sargable examples: `WHERE UPPER(email) = ?` (function on the column — index the expression instead or store normalized), `WHERE col + 0 = ?`, `WHERE col LIKE '%x'` (leading wildcard), implicit type casts (`WHERE varchar_col = 123`). Rewrite to compare the bare column, or create a functional/expression index.

### Why indexes slow writes
Every INSERT/UPDATE/DELETE must maintain every affected index (and keep it balanced). Too many indexes slow writes and bloat storage; index for the queries you actually run.

### Join strategies (conceptual)
The optimizer chooses nested-loop (good with an index on the inner side, small outer), hash join (large unsorted sets), or merge join (both sorted). You don't pick directly, but indexes and statistics steer the choice.

## C. Code — reading a plan and fixing a query
```sql
-- See the real plan WITH actual timings/rows (Postgres)
EXPLAIN (ANALYZE, BUFFERS) SELECT * FROM orders WHERE customer_id = 42 AND status = 'PAID';
-- Look for: Seq Scan (full scan) vs Index Scan; estimated vs actual rows (big gap = stale stats);
-- rows removed by filter (non-sargable predicate).
```
```sql
-- Composite index matching an equality+range query
CREATE INDEX idx_orders_cust_status_created
  ON orders (customer_id, status, created_at);  -- equality cols first, range last
-- Serves: WHERE customer_id=? AND status=? ORDER BY created_at
-- Does NOT serve: WHERE status=? alone (no leading customer_id)
```
```sql
-- NON-sargable -> index unused
SELECT * FROM users WHERE LOWER(email) = 'a@b.com';     -- function on column
-- FIX: expression index, or store a normalized column
CREATE INDEX idx_users_email_lower ON users (LOWER(email)); -- expression index (PG)
-- or normalize email on write and query the bare column.
```
Oracle: `EXPLAIN PLAN FOR ...; SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY);` or `DBMS_XPLAN.DISPLAY_CURSOR` for the actual plan.

## D. Trade-offs

| Decision | Pros | Cons | When |
|---|---|---|---|
| Add index | faster reads | slower writes, storage | selective, frequent query predicate |
| Composite index | serves multi-col queries | order-sensitive, bigger | known equality+range patterns |
| Covering/INCLUDE | index-only scan | larger index | hot read path returning few columns |
| Index low-cardinality col | — | usually ignored | rarely worth it alone (maybe partial index) |
| More indexes | more read options | write amplification | diminishing returns; prune unused |

## E. Interview Q&A

### IDX-01 (P1) — How does a B-tree index speed up queries, and what can't it help?
**Strong answer:** It's a sorted balanced tree, so equality, ranges, prefix `LIKE 'x%'`, `ORDER BY`, and `MIN/MAX` can be served in O(log n) + sequential. It can't help a leading-wildcard `LIKE '%x'`, a function applied to the column, or very low-selectivity predicates where a full scan is cheaper. I confirm usage with the execution plan.

<a id="idx-02"></a>
### IDX-02 (P0) — Why might a correct-looking composite index be ignored?
**Strong answer:** A composite index on `(a,b,c)` is sorted by `a` first, so it only serves queries that use a left prefix (`a`, then `a,b`, …). If my query filters on `b` alone or on `c`, the leading column `a` isn't constrained, so the index can't be traversed usefully and the optimizer falls back to a scan — like searching a phone book by first name. It can also be ignored if the predicate is non-sargable, if statistics are stale and mis-estimate selectivity, or if the predicate matches too many rows to beat a sequential scan. The fix is to order index columns to match the query (equality columns first, range last) and keep statistics fresh.
**Weak answer:** "The index is wrong." Usually the index is fine; the *column order* or *predicate shape* doesn't match.

### IDX-03 (P1) — What is a covering index?
**Strong answer:** An index that contains every column a query reads (key columns plus `INCLUDE`d payload), so the DB answers from the index alone (index-only scan) without visiting the table — a big win for hot, narrow read queries. The cost is a larger index and more write maintenance.

### IDX-04 (P1) — Why can adding an index make the system slower overall?
**Strong answer:** Indexes must be maintained on every write, so each extra index slows INSERT/UPDATE/DELETE and consumes storage; a write-heavy table with many indexes pays a real cost. Also, an index on a low-selectivity column is often ignored, so it's pure overhead. I index for the queries I actually run and periodically prune unused indexes.

<a id="idx-05"></a>
### IDX-05 (P0) — What is sargability? Give non-sargable predicates and fixes.
**Strong answer:** A predicate is sargable if an index can be used to satisfy it. Non-sargable patterns: a function on the column (`UPPER(col)=?`), arithmetic on the column (`col+0=?`), a leading-wildcard `LIKE '%x'`, and implicit type casts from a type mismatch. Fixes: compare the bare column (move functions to the parameter side), create an expression/functional index, store a normalized column, or restructure the search (full-text index for substring search). The goal is to let the predicate match the index's sorted key.

### IDX-06 (P1) — How do stale statistics cause bad plans, and how do you detect them?
**Strong answer:** The optimizer estimates row counts from statistics; if they're stale (after bulk loads/large changes), it mis-estimates selectivity and may pick a scan over an index or the wrong join. I detect it in `EXPLAIN ANALYZE` by a large gap between estimated and actual rows, and fix it by refreshing stats (`ANALYZE` in Postgres, `DBMS_STATS` in Oracle) and ensuring autovacuum/auto-stats are healthy.

### IDX-07 (P1) — How do you verify an index actually helped?
**Strong answer:** I capture the plan with real timings before and after (`EXPLAIN (ANALYZE, BUFFERS)` / `DBMS_XPLAN.DISPLAY_CURSOR`), confirm the plan switched from a sequential scan to an index/index-only scan, that actual rows match estimates, and that latency improved under representative data volume — not a tiny dev table. I never assume an index helped without the plan.

## F. Production relevance
Most "slow query" incidents are a missing or mis-ordered index, a non-sargable predicate, or stale statistics — and the N+1 problem ([12](../12-spring-data-jpa-and-hibernate/)) is the app-side cousin (a storm of small indexed lookups). The disciplined fix is evidence-based: read `EXPLAIN ANALYZE`, match index column order to query predicates (equality then range), make predicates sargable, add covering indexes for hot paths, keep statistics fresh, and prune write-amplifying unused indexes. "I'd confirm with the plan" is the phrase that signals seniority.

## G. Revision

### 10 key takeaways
1. B-tree indexes serve equality/range/prefix/ORDER BY; not leading-wildcard or functions.
2. Composite index serves a **left prefix** only — column order is everything.
3. Equality columns first, range column last.
4. Covering/INCLUDE indexes enable index-only scans.
5. Low-selectivity indexes are often ignored.
6. Sargable predicates compare the bare column; functions/casts/leading `%` break it.
7. Stale statistics cause bad plans; refresh them.
8. Every index slows writes — index for real queries, prune unused.
9. Read `EXPLAIN ANALYZE`: scan vs index, estimated vs actual rows.
10. Always verify with the plan under realistic data.

### 10 rapid-fire
1. Index for `LIKE '%x'`? (no) 2. Composite `(a,b)` serves `WHERE b=?`? (no) 3. Index-only scan needs? (covering index) 4. Low-cardinality index? (often ignored) 5. `UPPER(col)=?` sargable? (no) 6. Fix stale plans? (ANALYZE/DBMS_STATS) 7. Downside of many indexes? (write cost) 8. PG plan command? (EXPLAIN ANALYZE) 9. Order equality vs range? (equality first) 10. Verify improvement? (plan + timings)

### 5 tricky
1. Why ignored despite a matching-looking index? (column order/sargability/selectivity/stats) 2. Why index unused for `LOWER(email)`? (function) 3. Why did a plan regress after a bulk load? (stale stats) 4. Why can an index hurt a write-heavy table? 5. Why does low cardinality defeat an index?

### 5 exercises
1. Create a composite index and prove via plan it serves `a` and `a,b` but not `b` alone.
2. Make a non-sargable `UPPER()` query sargable with an expression index; compare plans.
3. Build a covering index and observe an index-only scan.
4. Corrupt plan quality with stale stats, then fix with ANALYZE; show estimated-vs-actual.
5. Convert a slow query to use keyset pagination + supporting index and verify.

### One-page revision sheet
> B-tree=sorted → equality/range/prefix 'x%'/ORDER BY/MIN-MAX; NOT '%x' or function-on-column. **Composite (a,b,c) = left-prefix only; order = equality cols first, range last.** Covering/INCLUDE → index-only scan. **Sargable** = compare bare column (no UPPER()/+0/cast/leading %); fix with expression index or normalized column. Low selectivity → ignored. Stale stats → bad plans (ANALYZE/DBMS_STATS); spot via estimated≠actual rows. Indexes slow writes → index real queries, prune unused. **Always EXPLAIN ANALYZE to verify.**

### Prerequisites to revisit
- SQL queries & pagination (ch.15); N+1 (ch.12); feeds the slow-query playbook (ch.22).
