package com.concurrency.locking.jvm;

import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class ReentrantLockTest {

    @Test
    void testBoundedBufferPutAndTakeWithTimeout() throws InterruptedException {
        ReentrantLockWithTimeoutAndConditions<Integer> buffer =
                new ReentrantLockWithTimeoutAndConditions<>(2);

        assertTrue(buffer.putWithTimeout(10, 100, TimeUnit.MILLISECONDS));
        assertTrue(buffer.putWithTimeout(20, 100, TimeUnit.MILLISECONDS));

        // Buffer is full (capacity 2), put should time out
        assertFalse(buffer.putWithTimeout(30, 100, TimeUnit.MILLISECONDS));

        assertEquals(10, buffer.takeWithTimeout(100, TimeUnit.MILLISECONDS));
        // Space freed, now put should succeed
        assertTrue(buffer.putWithTimeout(30, 100, TimeUnit.MILLISECONDS));
        assertEquals(2, buffer.size());
    }

    @Test
    void testConcurrentProducersAndConsumers() throws InterruptedException {
        int capacity = 5;
        int operations = 1000;
        ReentrantLockWithTimeoutAndConditions<Integer> buffer =
                new ReentrantLockWithTimeoutAndConditions<>(capacity);

        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch latch = new CountDownLatch(operations);

        // Producer task
        Runnable producer = () -> {
            for (int i = 0; i < operations; i++) {
                try {
                    while (!buffer.putWithTimeout(i, 50, TimeUnit.MILLISECONDS)) {
                        // retry until space available
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        };

        // Consumer task
        Runnable consumer = () -> {
            for (int i = 0; i < operations; i++) {
                try {
                    Integer val = null;
                    while (val == null) {
                        val = buffer.takeWithTimeout(50, TimeUnit.MILLISECONDS);
                    }
                    latch.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        };

        executor.submit(producer);
        executor.submit(consumer);

        boolean completed = latch.await(10, TimeUnit.SECONDS);
        assertTrue(completed, "All 1000 items should be produced and consumed successfully");
        executor.shutdownNow();
    }
}
