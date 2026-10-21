package com.concurrency.locking.jvm;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * <h1>LOCK-FREE vs LOCK STRIPING vs COARSE LOCKING</h1>
 *
 * <h2>Interview Question: "Why does AtomicLong degrade under extreme multi-threaded contention, and how does LongAdder fix it?"</h2>
 *
 * <h3>1. Coarse Locking (synchronized):</h3>
 * <ul>
 *   <li>Puts contending threads into OS wait queues (heavyweight monitor).</li>
 *   <li>High context-switch penalty.</li>
 * </ul>
 *
 * <h3>2. Lock-Free Atomic (CAS - Compare-And-Swap):</h3>
 * <ul>
 *   <li>{@link AtomicLong#incrementAndGet()} uses CPU assembly {@code CMPXCHG} in a spin loop.</li>
 *   <li><b>Cache Invalidation Bottleneck (MESI Protocol):</b> When 64 threads try to CAS the same 8-byte memory location,
 *       every successful CAS invalidates the L1/L2 cache line of the other 63 CPU cores.</li>
 *   <li>Result: Memory bus saturation and CPU cycles wasted spinning with zero progress ("CAS storm").</li>
 * </ul>
 *
 * <h3>3. Lock Striping & Striped Cells (LongAdder / Striped64):</h3>
 * <ul>
 *   <li>Internally maintains a table of {@code Cell} objects.</li>
 *   <li>Threads hash to different cells based on {@code Thread.currentThread().probe}.</li>
 *   <li>Each cell is padded with {@code @jdk.internal.vm.annotation.Contended} to occupy a full 64-byte CPU cache line,
 *       <b>preventing False Sharing</b> (where two unrelated variables share the same cache line).</li>
 *   <li>Under contention, cells are dynamically resized. Final sum aggregates all cells: {@code sum()}.</li>
 * </ul>
 */
public class LockFreeVsLockStripingExample {

    // 1. Coarse Lock
    private long synchronizedCounter = 0;
    private final Object lock = new Object();

    // 2. Lock-free Atomic CAS
    private final AtomicLong atomicCounter = new AtomicLong(0);

    // 3. Striped Cells (Highest throughput under heavy contention)
    private final LongAdder stripedCounter = new LongAdder();

    public void incrementSynchronized() {
        synchronized (lock) {
            synchronizedCounter++;
        }
    }

    public void incrementAtomic() {
        atomicCounter.incrementAndGet();
    }

    public void incrementStriped() {
        stripedCounter.increment();
    }

    public long getSynchronized() {
        synchronized (lock) {
            return synchronizedCounter;
        }
    }

    public long getAtomic() {
        return atomicCounter.get();
    }

    public long getStriped() {
        return stripedCounter.sum();
    }
}
