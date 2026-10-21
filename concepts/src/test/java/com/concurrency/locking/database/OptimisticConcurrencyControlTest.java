package com.concurrency.locking.database;

import com.concurrency.locking.database.entity.InventoryItem;
import com.concurrency.locking.database.repository.InventoryRepository;
import com.concurrency.locking.database.service.OptimisticInventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class OptimisticConcurrencyControlTest {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private OptimisticInventoryService optimisticInventoryService;

    private Long itemId;

    @BeforeEach
    void setup() {
        inventoryRepository.deleteAll();
        InventoryItem item = new InventoryItem("SKU-MACBOOK", 100);
        item = inventoryRepository.save(item);
        itemId = item.getId();
    }

    @Test
    void testConcurrentOptimisticDeductionWithRetries() throws InterruptedException {
        int threadCount = 10;
        int qtyPerThread = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successfulTransactions = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    boolean success = optimisticInventoryService.deductStockOptimistic(itemId, qtyPerThread);
                    if (success) {
                        successfulTransactions.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean done = latch.await(10, TimeUnit.SECONDS);
        assertTrue(done);

        InventoryItem updated = inventoryRepository.findById(itemId).orElseThrow();
        // Initial 100 - (successfulTransactions * 5)
        int expectedRemaining = 100 - (successfulTransactions.get() * qtyPerThread);
        assertEquals(expectedRemaining, updated.getAvailableStock().intValue());
        executor.shutdownNow();
    }
}
