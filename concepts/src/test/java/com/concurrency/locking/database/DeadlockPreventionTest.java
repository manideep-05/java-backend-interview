package com.concurrency.locking.database;

import com.concurrency.locking.database.entity.BankAccount;
import com.concurrency.locking.database.repository.BankAccountRepository;
import com.concurrency.locking.database.service.DeadlockFreeTransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class DeadlockPreventionTest {

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private DeadlockFreeTransferService deadlockFreeTransferService;

    private Long accountAId;
    private Long accountBId;

    @BeforeEach
    void setup() {
        bankAccountRepository.deleteAll();
        BankAccount accA = bankAccountRepository.save(new BankAccount("ACC-1001", new BigDecimal("10000.00")));
        BankAccount accB = bankAccountRepository.save(new BankAccount("ACC-1002", new BigDecimal("10000.00")));
        accountAId = accA.getId();
        accountBId = accB.getId();
    }

    @Test
    void testConcurrentCrossTransfersDoNotDeadlock() throws InterruptedException {
        int rounds = 20;
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch latch = new CountDownLatch(rounds * 2);
        AtomicInteger successfulTransfers = new AtomicInteger(0);

        // Half transfers A -> B, other half transfers B -> A simultaneously
        for (int i = 0; i < rounds; i++) {
            executor.submit(() -> {
                try {
                    boolean success = deadlockFreeTransferService.transferFundsDeadlockFree(
                            accountAId, accountBId, new BigDecimal("10.00"));
                    if (success) successfulTransfers.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });

            executor.submit(() -> {
                try {
                    boolean success = deadlockFreeTransferService.transferFundsDeadlockFree(
                            accountBId, accountAId, new BigDecimal("10.00"));
                    if (success) successfulTransfers.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        // Must complete in under 10 seconds without deadlocking
        boolean completed = latch.await(10, TimeUnit.SECONDS);
        assertTrue(completed, "Cross transfers must complete without deadlock");

        // The total net balance of both accounts combined must stay unchanged (20,000.00)
        BankAccount accA = bankAccountRepository.findById(accountAId).orElseThrow();
        BankAccount accB = bankAccountRepository.findById(accountBId).orElseThrow();
        BigDecimal totalSum = accA.getBalance().add(accB.getBalance());
        assertEquals(new BigDecimal("20000.00"), totalSum);

        executor.shutdownNow();
    }
}
