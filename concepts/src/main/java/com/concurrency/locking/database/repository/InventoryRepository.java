package com.concurrency.locking.database.repository;

import com.concurrency.locking.database.entity.InventoryItem;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<InventoryItem, Long> {

    Optional<InventoryItem> findByProductCode(String productCode);

    /**
     * PESSIMISTIC_WRITE translates to:
     * SELECT * FROM inventory_item WHERE id = ? FOR UPDATE
     *
     * In PostgreSQL / MySQL:
     * - Acquires exclusive row-level lock on the index/row.
     * - Any other transaction attempting to read (with FOR UPDATE/FOR SHARE) or write to this row blocks.
     *
     * QueryHint specifies lock timeout in milliseconds to prevent infinite hangs.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")})
    @Query("SELECT i FROM InventoryItem i WHERE i.id = :id")
    Optional<InventoryItem> findByIdWithPessimisticWriteLock(@Param("id") Long id);

    /**
     * PESSIMISTIC_READ translates to:
     * SELECT * FROM inventory_item WHERE id = ? FOR SHARE (or LOCK IN SHARE MODE)
     *
     * Allows other concurrent readers to read, but blocks writers.
     */
    @Lock(LockModeType.PESSIMISTIC_READ)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")})
    @Query("SELECT i FROM InventoryItem i WHERE i.id = :id")
    Optional<InventoryItem> findByIdWithPessimisticReadLock(@Param("id") Long id);

    /**
     * Production Pattern: Atomic DB-Level Conditional Decrement (Lock-Free DB Write).
     * Bypasses ORM entity state loading. The database engine's row-level lock ensures atomicity.
     * Returns: number of affected rows (1 if successful, 0 if insufficient stock).
     */
    @Modifying
    @Query("UPDATE InventoryItem i SET i.availableStock = i.availableStock - :quantity " +
           "WHERE i.id = :id AND i.availableStock >= :quantity")
    int atomicDecrementStock(@Param("id") Long id, @Param("quantity") int quantity);
}
