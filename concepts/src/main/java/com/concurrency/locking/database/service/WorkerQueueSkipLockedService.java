package com.concurrency.locking.database.service;

import com.concurrency.locking.database.entity.JobTask;
import com.concurrency.locking.database.repository.JobTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * <h1>CONCURRENT WORKER QUEUE: {@code SELECT ... FOR UPDATE SKIP LOCKED}</h1>
 *
 * <h2>Interview Problem:</h2>
 * <p>"You have 10 microservice worker nodes consuming background jobs from an RDBMS table.
 * If all 10 workers run `SELECT * FROM tasks WHERE status = 'PENDING' LIMIT 1 FOR UPDATE`,
 * Worker 1 locks row 1, and Workers 2-10 BLOCK on row 1! Only after Worker 1 finishes do Workers 2-10 resume,
 * meaning work is serialized instead of parallelized! How do you solve this without introducing RabbitMQ/Kafka?"</p>
 *
 * <h2>The Solution: {@code SKIP LOCKED}</h2>
 * <ul>
 *   <li>Worker 1 locks row 1.</li>
 *   <li>Worker 2 executes query: DB skips locked row 1 and gives Worker 2 row 2 immediately!</li>
 *   <li>Worker 3 gets row 3 immediately!</li>
 *   <li>Zero blocking, zero thread queuing, perfect parallelism!</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkerQueueSkipLockedService {

    private final JobTaskRepository jobTaskRepository;

    @Transactional
    public Optional<JobTask> pollAndProcessNextJob(String workerId) {
        // Skips rows currently locked by other workers
        Optional<JobTask> taskOpt = jobTaskRepository.findNextPendingJobSkipLocked();

        if (taskOpt.isEmpty()) {
            return Optional.empty();
        }

        JobTask task = taskOpt.get();
        task.setStatus(JobTask.JobStatus.PROCESSING);
        task.setLockedBy(workerId);
        task.setLockedAt(LocalDateTime.now());
        jobTaskRepository.save(task);

        log.info("Worker [{}] claimed task [{}] for processing", workerId, task.getId());
        return Optional.of(task);
    }

    @Transactional
    public void completeJob(Long taskId) {
        JobTask task = jobTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
        task.setStatus(JobTask.JobStatus.COMPLETED);
        task.setCompletedAt(LocalDateTime.now());
        jobTaskRepository.save(task);
        log.info("Task [{}] marked as COMPLETED", taskId);
    }
}
