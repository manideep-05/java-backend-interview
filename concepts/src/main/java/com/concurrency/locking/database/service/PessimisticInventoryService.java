package com.concurrency.locking.database.service;

import com.concurrency.locking.database.entity.InventoryItem;
import com.concurrency.locking.database.repository.InventoryRepository;
import jakarta.persistence.PessimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h1>DATABASE PESSIMISTIC LOCKING PATTERN</h1>
 *
 * <h2>Mechanism:</h2>
 * <ul>
 *   <li>Issues {@code SELECT ... FOR UPDATE} inside a database transaction.</li>
 *   <li>The database engine (InnoDB / Postgres MVCC) places an exclusive row-level lock on the index record.</li>
 *   <li>Other concurrent transactions attempting to read/modify this row <b>queue up and block</b> until the transaction commits or rolls back.</li>
 * </ul>
 *
 * <h2>When to Implement:</h2>
 * <ul>
 *   <li><b>High Contention / High Write Frequency:</b> Where optimistic lock retries would saturate DB CPU and waste bandwidth.</li>
 *   <li><b>Zero-Retry SLA:</b> Workflows where client requests cannot afford retry latency (e.g. Flight Seat Reservation checkout).</li>
 * </ul>
 *
 * <h2>CRITICAL PRODUCTION RULES:</h2>
 * <ol>
 *   <li><b>Always specify a Lock Timeout:</b> Without a timeout, a hanging transaction can block all worker threads indefinitely,
 *       leading to connection pool exhaustion (HikariCP starvation).</li>
 *   <li><b>Zero I/O inside transaction:</b> NEVER make external HTTP REST calls, gRPC requests, or send Kafka messages
 *       while holding a pessimistic DB lock!</li>
 *   <li><b>Transaction Duration:</b> Keep transaction under 5-10 milliseconds max.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PessimisticInventoryService {

    private final InventoryRepository inventoryRepository;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public boolean deductStockPessimistic(Long itemId, int quantity) {
        log.info("Acquiring pessimistic write lock for item: {}, qty: {}", itemId, quantity);
        try {
            // Locks the row exclusively; waits up to lock timeout
            InventoryItem item = inventoryRepository.findByIdWithPessimisticWriteLock(itemId)
                    .orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemId));

            if (item.getAvailableStock() < quantity) {
                log.warn("Pessimistic: Insufficient stock for item: {}. Available: {}, Requested: {}",
                        itemId, item.getAvailableStock(), quantity);
                return false;
            }

            item.setAvailableStock(item.getAvailableStock() - quantity);
            inventoryRepository.save(item);
            log.info("Pessimistic deduction completed for item: {}, remaining: {}", itemId, item.getAvailableStock());
            return true;
        } catch (PessimisticLockException ex) {
            log.error("Pessimistic lock timeout exceeded for item: {}", itemId, ex);
            throw ex;
        }
    }

    /**
     * Alternative High-Performance Pattern: Direct Atomic SQL Update.
     * Avoids entity hydration in JPA and bypasses SELECT FOR UPDATE overhead.
     * The DB row update locks the row momentarily only for the duration of the single statement.
     */
    @Transactional
    public boolean deductStockAtomicSql(Long itemId, int quantity) {
        int updatedRows = inventoryRepository.atomicDecrementStock(itemId, quantity);
        return updatedRows > 0;
    }
}
