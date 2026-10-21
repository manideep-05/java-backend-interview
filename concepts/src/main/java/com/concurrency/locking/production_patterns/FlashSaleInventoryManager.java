package com.concurrency.locking.production_patterns;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * <h1>PRODUCTION ARCHITECTURE: FLASH SALE HIGH-CONCURRENCY INVENTORY DEDUCTION</h1>
 *
 * <h2>The SDE-2 Interview Question:</h2>
 * <p><b>"We have a Flash Sale: 1,000 units of iPhone 16. 200,000 concurrent users click 'Buy Now' within 3 seconds.
 * How would you design the inventory deduction system to prevent overselling, maintain sub-50ms latency,
 * and protect the database from crashing?"</b></p>
 *
 * <h2>Why Standard Approaches Fail:</h2>
 * <ul>
 *   <li><b>Optimistic Locking (@Version):</b> 1 request succeeds, 199,999 fail with {@code OptimisticLockException}.
 *       If retried, the retry storm creates an avalanche that completely crashes the database.</li>
 *   <li><b>Pessimistic Locking (SELECT ... FOR UPDATE):</b> 200,000 requests queue up for 1 single database row.
 *       Connection pools (HikariCP) run out of connections within 200ms; thread pool exhausts; system times out.</li>
 * </ul>
 *
 * <h2>The 3-Tier Production Architecture:</h2>
 * <ol>
 *   <li><b>Tier 1 (In-Memory Atomic Gate - Redis Lua):</b> Pre-warm inventory in Redis.
 *       Atomic Lua script checks {@code stock >= requested} and decrements in RAM (100,000+ ops/sec at sub-5ms latency).
 *       Once stock hits 0, all subsequent 199,000 requests are rejected at the edge without touching the DB!</li>
 *   <li><b>Tier 2 (Inventory Bucketing / Striping in DB):</b> If directly hitting DB, divide stock into N buckets
 *       (e.g. {@code iphone_bucket_0} through {@code iphone_bucket_9}). Contention is divided by N!</li>
 *   <li><b>Tier 3 (Async Event Persistence):</b> Successful Redis decrements emit an event to Kafka/RabbitMQ
 *       for asynchronous order creation and DB inventory sync.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FlashSaleInventoryManager {

    private final StringRedisTemplate redisTemplate;

    // Redis Lua Script: Atomically checks stock and decrements if sufficient
    private static final String ATOMIC_STOCK_DECREMENT_LUA =
            "local current_stock = tonumber(redis.call('get', KEYS[1])) " +
            "if current_stock == nil then " +
            "    return -1 " + // Item not found
            "end " +
            "local requested_qty = tonumber(ARGV[1]) " +
            "if current_stock >= requested_qty then " +
            "    redis.call('decrby', KEYS[1], requested_qty) " +
            "    return current_stock - requested_qty " + // Success, return remaining stock
            "else " +
            "    return -2 " + // Insufficient stock
            "end";

    private final DefaultRedisScript<Long> decrementScript =
            new DefaultRedisScript<>(ATOMIC_STOCK_DECREMENT_LUA, Long.class);

    /**
     * Pre-warms inventory in Redis before the Flash Sale begins.
     */
    public void prewarmStock(String productCode, int totalStock) {
        String key = "flash_sale:stock:" + productCode;
        redisTemplate.opsForValue().set(key, String.valueOf(totalStock));
        log.info("Pre-warmed inventory in Redis for product {}: {} units", productCode, totalStock);
    }

    /**
     * High-speed atomic inventory deduction at Tier 1.
     *
     * @return remaining stock on success, -1 if not found, -2 if sold out / insufficient
     */
    public DeductResult deductFlashSaleStock(String productCode, int quantity) {
        String key = "flash_sale:stock:" + productCode;

        Long result = redisTemplate.execute(
                decrementScript,
                Collections.singletonList(key),
                String.valueOf(quantity)
        );

        if (result == null || result == -1L) {
            return new DeductResult(false, 0, "Product not found or not active");
        } else if (result == -2L) {
            return new DeductResult(false, 0, "SOLD OUT! Insufficient stock");
        } else {
            return new DeductResult(true, result, "Successfully reserved stock");
        }
    }

    public record DeductResult(boolean success, long remainingStock, String message) {}
}
