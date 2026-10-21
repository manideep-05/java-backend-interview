package com.concurrency.locking.jvm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReadWriteLockTest {

    @Test
    void testComputeIfAbsentWithLockDowngrade() {
        ReadWriteLockCacheExample<String, String> cache = new ReadWriteLockCacheExample<>();

        // First access computes value and downgrades lock
        String val = cache.computeIfAbsentWithDowngrade("user:123", k -> "Alice");
        assertEquals("Alice", val);

        // Second access hits read lock fast-path
        String cachedVal = cache.get("user:123");
        assertEquals("Alice", cachedVal);
    }
}
