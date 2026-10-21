package com.concurrency.locking.database.service;

import com.concurrency.locking.database.entity.InventoryItem;
import com.concurrency.locking.database.repository.InventoryRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h1>DATABASE OPTIMISTIC LOCKING PATTERN</h1>
 *
 * <h2>Mechanism:</h2>
 * <ul>
 *   <li>Relies on the {@code @Version} column.</li>
 *   <li>No row locks are held during the transaction!</li>
 *   <li>On commit, Hibernate issues:
 *       {@code UPDATE inventory_item SET available_stock = ?, version = version + 1 WHERE id = ? AND version = ?}</li>
 *   <li>If another transaction committed an update first, the version in DB has already changed,
 *       so the WHERE clause matches 0 rows. Hibernate detects this and throws {@link OptimisticLockException}.</li>
 * </ul>
 *
 * <h2>When to Implement:</h2>
 * <ul>
 *   <li><b>Low to Medium Contention:</b> Read-heavy workloads with infrequent concurrent updates (e.g. Editing user profile, updating product catalogue).</li>
 *   <li><b>Long-running user workflows:</b> User opens an edit page in browser for 2 minutes before clicking "Save".
 *       (Holding a DB row lock for 2 minutes is unacceptable; optimistic lock allows version comparison on save).</li>
 * </ul>
 *
 * <h2>When NOT to Implement:</h2>
 * <ul>
 *   <li><b>High Contention / Flash Sales:</b> If 1,000 users try to buy 1 iPhone simultaneously, 1 transaction wins and
 *       999 transactions fail with OptimisticLockException. Retrying 999 times causes exponential DB load and massive latency spikes.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OptimisticInventoryService {

    private final InventoryRepository inventoryRepository;

    /**
     * Deducts stock with automatic exponential backoff and jitter retry.
     * Jitter (random = true) prevents the "Thundering Herd" retry collision problem.
     */
    @Retryable(
            retryFor = {ObjectOptimisticLockingFailureException.class, OptimisticLockException.class},
            maxAttempts = 4,
            backoff = @Backoff(delay = 50, multiplier = 2.0, maxDelay = 500, random = true)
    )
    @Transactional
    public boolean deductStockOptimistic(Long itemId, int quantity) {
        log.info("Attempting optimistic stock deduction for item: {}, qty: {}", itemId, quantity);
        InventoryItem item = inventoryRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemId));

        if (item.getAvailableStock() < quantity) {
            log.warn("Insufficient stock for item: {}. Available: {}, Requested: {}",
                    itemId, item.getAvailableStock(), quantity);
            return false;
        }

        item.setAvailableStock(item.getAvailableStock() - quantity);
        inventoryRepository.save(item);
        log.info("Optimistic deduction succeeded for item: {}, new stock: {}, version: {}",
                itemId, item.getAvailableStock(), item.getVersion());
        return true;
    }

    /**
     * Fallback recovery method executed when all retry attempts fail due to OptimisticLockException.
     */
    @Recover
    public boolean recoverFromOptimisticLockException(Exception ex, Long itemId, int quantity) {
        log.error("Exhausted all retry attempts for item: {} due to high contention. Error: {}",
                itemId, ex.getMessage());
        return false;
    }
}
