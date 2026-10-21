package com.concurrency.locking.distributed.db_advisory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

/**
 * <h1>DATABASE ADVISORY LOCKS: POSTGRESQL</h1>
 *
 * <h2>Interview Problem:</h2>
 * <p>"Our team has a PostgreSQL database and 5 Spring Boot backend instances.
 * We need a distributed lock to run a midnight cleanup batch job only once across the cluster.
 * The DevOps team refused to spin up a Redis or ZooKeeper cluster due to cost/maintenance.
 * How can we achieve distributed locking without any new infrastructure?"</p>
 *
 * <h2>The Solution: PostgreSQL Advisory Locks:</h2>
 * <ul>
 *   <li>Advisory locks are <b>application-level locks maintained in PostgreSQL shared memory</b>.</li>
 *   <li>They <b>do NOT lock any table, row, or schema</b>.</li>
 *   <li>Two variants:
 *       <ol>
 *         <li><b>Session-level:</b> {@code pg_try_advisory_lock(key)} - Held until explicitly unlocked or connection closes.</li>
 *         <li><b>Transaction-level:</b> {@code pg_advisory_xact_lock(key)} - Automatically released when transaction ends (COMMIT/ROLLBACK).
 *             Zero risk of lock leaks!</li>
 *       </ol>
 *   </li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostgresAdvisoryLockService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Executes logic guarded by a PostgreSQL transaction-scoped advisory lock.
     * The lock is automatically released by PostgreSQL when this Spring transaction finishes.
     */
    @Transactional
    public <T> T executeWithTransactionAdvisoryLock(long lockId, Supplier<T> task) {
        log.info("Acquiring PostgreSQL transaction advisory lock for key: {}", lockId);

        // Blocks until lock acquired, auto-releases on commit/rollback
        jdbcTemplate.execute("SELECT pg_advisory_xact_lock(" + lockId + ")");

        log.info("Acquired PostgreSQL advisory lock for key: {}", lockId);
        return task.get();
    }

    /**
     * Non-blocking attempt using pg_try_advisory_lock.
     * Returns true if lock was successfully acquired, false if held by another node.
     */
    public boolean tryAcquireSessionAdvisoryLock(long lockId) {
        Boolean acquired = jdbcTemplate.queryForObject(
                "SELECT pg_try_advisory_lock(?)",
                Boolean.class,
                lockId
        );
        return Boolean.TRUE.equals(acquired);
    }

    public boolean releaseSessionAdvisoryLock(long lockId) {
        Boolean released = jdbcTemplate.queryForObject(
                "SELECT pg_advisory_unlock(?)",
                Boolean.class,
                lockId
        );
        return Boolean.TRUE.equals(released);
    }
}
