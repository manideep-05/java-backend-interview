package com.concurrency.locking.database.repository;

import com.concurrency.locking.database.entity.JobTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JobTaskRepository extends JpaRepository<JobTask, Long> {

    /**
     * SELECT ... FOR UPDATE SKIP LOCKED
     *
     * <h2>Under the Hood:</h2>
     * <ul>
     *   <li>Standard {@code FOR UPDATE} blocks until another transaction releases the lock.</li>
     *   <li>{@code NOWAIT} throws an immediate error if the row is locked.</li>
     *   <li><b>{@code SKIP LOCKED}</b> simply skips past already locked rows and returns the next unlocked row matching the WHERE criteria!</li>
     * </ul>
     *
     * <h2>Production Use Case:</h2>
     * <p>Building concurrent worker pools / job queues on top of relational databases without external brokers (Kafka/RabbitMQ)
     * and without lock contention or deadlocks.</p>
     */
    @Query(value = "SELECT * FROM job_task WHERE status = 'PENDING' ORDER BY id ASC LIMIT 1 FOR UPDATE SKIP LOCKED",
           nativeQuery = true)
    Optional<JobTask> findNextPendingJobSkipLocked();
}
