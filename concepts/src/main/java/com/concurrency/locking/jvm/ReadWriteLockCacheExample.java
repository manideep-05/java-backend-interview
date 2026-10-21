package com.concurrency.locking.jvm;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <h1>JVM READ-WRITE LOCK: {@code ReentrantReadWriteLock}</h1>
 *
 * <h2>Under the Hood:</h2>
 * <ul>
 *   <li>The 32-bit AQS {@code state} integer is split into two 16-bit numbers:
 *       - <b>High 16 bits:</b> Shared read lock count (allows up to 65,535 concurrent readers).
 *       - <b>Low 16 bits:</b> Exclusive write lock hold count (allows up to 65,535 reentrant write locks).</li>
 *   <li>Multiple threads can hold the read lock concurrently (Shared mode).</li>
 *   <li>Only one thread can hold the write lock (Exclusive mode), blocking all readers and other writers.</li>
 * </ul>
 *
 * <h2>Lock Downgrading vs Upgrading (CRITICAL SDE-2 INTERVIEW GOTCHA):</h2>
 * <ul>
 *   <li><b>Lock Downgrading is SUPPORTED:</b>
 *       Acquire Write Lock -> Acquire Read Lock -> Release Write Lock -> Use resource -> Release Read Lock.
 *       Why? Allows safely holding the read lock after modifying state without another writer jumping in between.</li>
 *   <li><b>Lock Upgrading is NOT SUPPORTED (Deadlock prone):</b>
 *       If two threads hold a Read Lock and both try to acquire a Write Lock, both block waiting for the other's Read Lock to release.
 *       Hence, JVM deliberately disallows direct upgrade. You MUST release the read lock first!</li>
 * </ul>
 *
 * <h2>Drawback: Write Starvation</h2>
 * <p>Under high read contention, incoming readers continuously acquire the read lock, starving writers indefinitely.
 * StampedLock (Java 8) addresses this with optimistic reads.</p>
 */
public class ReadWriteLockCacheExample<K, V> {

    private final Map<K, V> cache = new HashMap<>();
    private final ReentrantReadWriteLock rrwl = new ReentrantReadWriteLock();
    private final Lock readLock = rrwl.readLock();
    private final Lock writeLock = rrwl.writeLock();

    public V get(K key) {
        readLock.lock();
        try {
            return cache.get(key);
        } finally {
            readLock.unlock();
        }
    }

    public void put(K key, V value) {
        writeLock.lock();
        try {
            cache.put(key, value);
        } finally {
            writeLock.unlock();
        }
    }

    public void remove(K key) {
        writeLock.lock();
        try {
            cache.remove(key);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Production Pattern: Double-Checked Lock with Lock Downgrading.
     * Checks if key exists under read lock; if missing, releases read lock, acquires write lock,
     * populates value, downgrades to read lock, and returns.
     */
    public V computeIfAbsentWithDowngrade(K key, java.util.function.Function<K, V> mappingFunction) {
        readLock.lock();
        V value = cache.get(key);
        if (value != null) {
            readLock.unlock();
            return value;
        }

        // Must release read lock before acquiring write lock (Cannot upgrade!)
        readLock.unlock();

        writeLock.lock();
        try {
            // Re-check state inside write lock (Double-Check idiom)
            value = cache.get(key);
            if (value == null) {
                value = mappingFunction.apply(key);
                cache.put(key, value);
            }

            // Lock Downgrade: acquire read lock before releasing write lock
            readLock.lock();
        } finally {
            // Release write lock; we still hold read lock
            writeLock.unlock();
        }

        try {
            return value;
        } finally {
            readLock.unlock();
        }
    }
}
