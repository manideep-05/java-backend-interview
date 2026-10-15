-- 03_indexing_and_plans.sql — chapter 16. Run after 01_schema_and_seed.sql.
-- Best observed on a large table; add rows so the optimizer prefers an index over a seq scan.

-- Bulk up orders so plans are meaningful (PostgreSQL generate_series).
INSERT INTO orders(customer_id, status, amount)
SELECT (g % 3) + 1,
       (ARRAY['NEW','PAID','CANCELLED'])[(g % 3) + 1],
       (g % 1000)
FROM generate_series(1, 200000) AS g;   -- Oracle: use CONNECT BY LEVEL <= 200000

ANALYZE orders;   -- refresh statistics (Oracle: DBMS_STATS.GATHER_TABLE_STATS)

-- (A) Composite index: order = equality columns first, range/sort last.
CREATE INDEX idx_orders_cust_status_created ON orders (customer_id, status, created_at);

-- Served by the index (left prefix customer_id[, status]):
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM orders WHERE customer_id = 2 AND status = 'PAID' ORDER BY created_at;

-- NOT served efficiently (no leading customer_id) -> expect a sequential scan:
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM orders WHERE status = 'PAID';

-- (B) Sargability: a function on the column defeats the index.
-- Non-sargable (function on column) -> seq scan:
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM orders WHERE CAST(created_at AS DATE) = DATE '2026-01-01';

-- Sargable rewrite (range on the bare column) -> index-friendly:
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM orders
WHERE created_at >= TIMESTAMP '2026-01-01 00:00:00'
  AND created_at <  TIMESTAMP '2026-01-02 00:00:00';

-- How to read the plan: look for "Index Scan"/"Index Only Scan" vs "Seq Scan",
-- and compare the planner's estimated rows to the actual rows (a big gap => stale stats).

-- Oracle: EXPLAIN PLAN FOR <query>;  then  SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY);
--         or DBMS_XPLAN.DISPLAY_CURSOR for the actual executed plan.
