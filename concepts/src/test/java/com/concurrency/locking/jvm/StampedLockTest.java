package com.concurrency.locking.jvm;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StampedLockTest {

    @Test
    void testOptimisticReadCalculation() {
        StampedLockOptimisticExample point = new StampedLockOptimisticExample(3.0, 4.0);
        // sqrt(3^2 + 4^2) = 5.0
        assertEquals(5.0, point.distanceFromOrigin(), 0.0001);
    }

    @Test
    void testConcurrentWritesAndOptimisticReads() throws InterruptedException {
        StampedLockOptimisticExample point = new StampedLockOptimisticExample(0, 0);
        int threadCount = 10;
        int iterations = 500;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // Half writers, half readers
        for (int i = 0; i < threadCount / 2; i++) {
            executor.submit(() -> {
                for (int j = 0; j < iterations; j++) {
                    point.move(1.0, 1.0);
                }
                latch.countDown();
            });
        }

        for (int i = 0; i < threadCount / 2; i++) {
            executor.submit(() -> {
                for (int j = 0; j < iterations; j++) {
                    double dist = point.distanceFromOrigin();
                    assertTrue(dist >= 0);
                }
                latch.countDown();
            });
        }

        boolean finished = latch.await(10, TimeUnit.SECONDS);
        assertTrue(finished);
        executor.shutdownNow();
    }
}
