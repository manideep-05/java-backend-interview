-- 01_schema_and_seed.sql — sample schema + seed (PostgreSQL 16; Oracle notes inline)
-- Safe to re-run: drops then recreates.

DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS employee;
DROP TABLE IF EXISTS customer;
DROP TABLE IF EXISTS account;

-- Oracle: no "IF EXISTS" on older versions; use a PL/SQL block or ignore ORA-00942.

CREATE TABLE customer (
    id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,  -- Oracle 12c+: same syntax; or a SEQUENCE
    name VARCHAR(100) NOT NULL
);

CREATE TABLE orders (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customer(id),
    status      VARCHAR(20) NOT NULL,       -- 'NEW' | 'PAID' | 'CANCELLED'
    amount      NUMERIC(12,2) NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT now()  -- Oracle: SYSTIMESTAMP / CURRENT_TIMESTAMP
);

CREATE TABLE employee (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    department_id INT NOT NULL,
    salary        NUMERIC(12,2) NOT NULL
);

CREATE TABLE account (
    id      BIGINT PRIMARY KEY,
    balance NUMERIC(12,2) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0        -- for the optimistic-locking demo
);

INSERT INTO customer(name) VALUES ('Ada'), ('Linus'), ('Grace');  -- Grace will have no orders

INSERT INTO orders(customer_id, status, amount) VALUES
    (1, 'PAID', 100.00),
    (1, 'NEW',   40.00),
    (2, 'PAID',  75.00),
    (2, 'CANCELLED', 10.00);

INSERT INTO employee(name, department_id, salary) VALUES
    ('Alice', 1, 150000), ('Bob', 1, 120000), ('Cara', 1, 120000),
    ('Dan', 2, 200000),  ('Eve', 2, 180000);

INSERT INTO account(id, balance) VALUES (1, 100.00), (2, 100.00);
