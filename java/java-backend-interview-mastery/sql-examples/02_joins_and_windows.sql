-- 02_joins_and_windows.sql — chapter 15. Run after 01_schema_and_seed.sql.

-- (A) The LEFT JOIN "WHERE" trap --------------------------------------------
-- WRONG: predicate on the right table in WHERE silently turns LEFT into INNER
--        (customers with no PAID order disappear).
SELECT c.name, o.id AS order_id
FROM customer c
LEFT JOIN orders o ON o.customer_id = c.id
WHERE o.status = 'PAID';     -- Grace (no orders) and NEW/CANCELLED-only customers vanish

-- RIGHT: keep the outer rows by moving the condition into ON.
SELECT c.name, o.id AS order_id
FROM customer c
LEFT JOIN orders o ON o.customer_id = c.id AND o.status = 'PAID';  -- Grace appears with NULL order

-- (B) Anti-join with NOT EXISTS (NULL-safe; prefer over NOT IN) ---------------
SELECT c.id, c.name
FROM customer c
WHERE NOT EXISTS (SELECT 1 FROM orders o WHERE o.customer_id = c.id);  -- -> Grace

-- (C) WHERE vs HAVING ---------------------------------------------------------
-- Departments with more than 1 employee earning > 100000.
SELECT department_id, COUNT(*) AS cnt
FROM employee
WHERE salary > 100000        -- row filter BEFORE grouping
GROUP BY department_id
HAVING COUNT(*) > 1;         -- group filter AFTER aggregation

-- (D) Window functions: second-highest salary per department -----------------
SELECT department_id, name, salary
FROM (
    SELECT department_id, name, salary,
           DENSE_RANK() OVER (PARTITION BY department_id ORDER BY salary DESC) AS rnk
    FROM employee
) t
WHERE rnk = 2;               -- ties handled by DENSE_RANK

-- (E) Running total per customer by order time -------------------------------
SELECT customer_id, id AS order_id, amount,
       SUM(amount) OVER (PARTITION BY customer_id ORDER BY created_at, id
                         ROWS UNBOUNDED PRECEDING) AS running_total
FROM orders
ORDER BY customer_id, created_at, id;

-- Oracle notes: identical window-function syntax; DENSE_RANK/SUM OVER are standard.
