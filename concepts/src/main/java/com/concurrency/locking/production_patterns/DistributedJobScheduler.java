package com.concurrency.locking.production_patterns;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * <h1>PRODUCTION PATTERN: DISTRIBUTED LEADER CRON EXECUTION</h1>
 *
 * <h2>Interview Problem:</h2>
 * <p>"You deploy your Spring Boot microservice with 10 replicas on Kubernetes.
 * You have a {@code @Scheduled(cron = "0 0 * * * *")} hourly billing generation task.
 * When the clock strikes, all 10 replicas run the cron job simultaneously, billing customers 10 times!
 * How do you guarantee that EXACTLY ONE instance runs the job, while preserving high availability
 * if that instance crashes halfway through?"</p>
 *
 * <h2>The Solution: Distributed Leader Lock with Lease:</h2>
 * <ul>
 *   <li>Only the node that successfully acquires the distributed lock with {@code waitTime = 0} executes the job.</li>
 *   <li>The other 9 instances immediately skip without waiting, avoiding thread blocking.</li>
 *   <li>A bounded lease time (e.g. 5 minutes) ensures that if the executing node crashes, the lock will auto-expire.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DistributedJobScheduler {

    private final RedissonClient redissonClient;
    private static final String BILLING_CRON_LOCK_KEY = "scheduler:lock:hourly_billing";

    /**
     * Runs every hour across a multi-node Kubernetes cluster.
     * Guaranteed single execution across all cluster nodes.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void runClusterWideHourlyBilling() {
        RLock lock = redissonClient.getLock(BILLING_CRON_LOCK_KEY);

        try {
            // waitTime = 0: Do NOT wait if another node already grabbed the lock
            // leaseTime = 10 mins: Automatically release in case the worker crashes
            boolean acquired = lock.tryLock(0, 10, TimeUnit.MINUTES);

            if (!acquired) {
                log.info("Hourly billing job already claimed by another node in the cluster. Skipping execution.");
                return;
            }

            try {
                log.info("Node [{}] won the leader lock! Starting hourly billing process at {}",
                        Thread.currentThread().getName(), LocalDateTime.now());

                // Execute the critical batch billing business logic
                performBillingBatch();

            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                    log.info("Node [{}] successfully completed billing and released lock.",
                            Thread.currentThread().getName());
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Scheduler interrupted while trying to claim job lock", e);
        }
    }

    private void performBillingBatch() {
        // Business logic placeholder
        log.info("Executing billing computations...");
    }
}
