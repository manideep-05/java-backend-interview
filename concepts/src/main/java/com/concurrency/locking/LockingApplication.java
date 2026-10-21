package com.concurrency.locking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main entry point for the Locking and Concurrency Demonstration Service.
 * Demonstrates:
 * 1. JVM-level locking (Synchronized, ReentrantLock, ReadWriteLock, StampedLock)
 * 2. Database-level locking (Optimistic @Version, Pessimistic PESSIMISTIC_WRITE/READ, SKIP LOCKED)
 * 3. Distributed-level locking (Redis Redisson Fair/Watchdog, Zookeeper Curator, Fencing Tokens)
 */
@SpringBootApplication
@EnableRetry
@EnableAsync
public class LockingApplication {

    public static void main(String[] args) {
        SpringApplication.run(LockingApplication.class, args);
    }
}
