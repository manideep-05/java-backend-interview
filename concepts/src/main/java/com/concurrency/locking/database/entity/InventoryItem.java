package com.concurrency.locking.database.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "inventory_item")
@Getter
@Setter
@NoArgsConstructor
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String productCode;

    @Column(nullable = false)
    private Integer availableStock;

    /**
     * Optimistic Locking Version field.
     * Hibernate/JPA automatically checks:
     * UPDATE inventory_item SET available_stock = ?, version = version + 1
     * WHERE id = ? AND version = ?
     * If 0 rows updated, throws OptimisticLockException.
     */
    @Version
    private Long version;

    public InventoryItem(String productCode, Integer availableStock) {
        this.productCode = productCode;
        this.availableStock = availableStock;
    }
}
