package com.concurrency.locking.jvm;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <h1>JVM EXPLICIT LOCK: {@code ReentrantLock} & Condition Variables</h1>
 *
 * <h2>Under the Hood (AQS - AbstractQueuedSynchronizer):</h2>
 * <ul>
 *   <li>Backed by <b>AbstractQueuedSynchronizer (AQS)</b> using an atomic 32-bit integer {@code state} and a FIFO queue (CLH node variant).</li>
 *   <li>When a thread calls {@code lock()}:
 *       - In <b>Unfair mode (default):</b> Thread attempts immediate CAS on state (0 -> 1) without checking if queue has waiting threads ("barging").
 *         Provides <b>significantly higher throughput</b> because unparking a thread has OS scheduler delay (~microseconds),
 *         and an active CPU thread can do the work immediately.
 *       - In <b>Fair mode:</b> Checks {@code hasQueuedPredecessors()} first. If other threads are waiting, it enqueues itself.
 *         Prevents starvation at the cost of ~2x-10x throughput degradation.</li>
 * </ul>
 *
 * <h2>Production Superpowers over {@code synchronized}:</h2>
 * <ol>
 *   <li><b>Timed lock acquisition:</b> {@code tryLock(long timeout, TimeUnit unit)} - Critical for SLAs to avoid cascading thread hangs.</li>
 *   <li><b>Interruptible lock:</b> {@code lockInterruptibly()} - Allows cancellation when user aborts HTTP request.</li>
 *   <li><b>Multiple Condition Variables:</b> Separate wait-sets (e.g. {@code notEmpty} vs {@code notFull}), eliminating the "Thundering Herd"
 *       and spurious wakeup inefficiency of {@code Object.notifyAll()}.</li>
 * </ol>
 *
 * <h2>Golden Rule for Production:</h2>
 * <pre>{@code
 * lock.lock(); // MUST be called OUTSIDE the try block
 * try {
 *     // critical section
 * } finally {
 *     lock.unlock(); // MUST be in finally block
 * }
 * }</pre>
 * (Why outside `try`? If lock acquisition fails or throws an unchecked error, placing it inside `try` causes `unlock()` in finally to fail with `IllegalMonitorStateException`, masking the root cause!)
 */
public class ReentrantLockWithTimeoutAndConditions<T> {

    private final Queue<T> queue = new LinkedList<>();
    private final int capacity;

    // Unfair by default for high throughput
    private final ReentrantLock lock;
    private final Condition notFull;
    private final Condition notEmpty;

    public ReentrantLockWithTimeoutAndConditions(int capacity, boolean fair) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.capacity = capacity;
        this.lock = new ReentrantLock(fair);
        this.notFull = this.lock.newCondition();
        this.notEmpty = this.lock.newCondition();
    }

    public ReentrantLockWithTimeoutAndConditions(int capacity) {
        this(capacity, false);
    }

    /**
     * Bounded Put with SLA timeout. Returns false if timeout elapsed without capacity.
     */
    public boolean putWithTimeout(T item, long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        // Acquire lock with timeout
        if (!lock.tryLock(timeout, unit)) {
            return false; // Could not acquire lock in time
        }

        try {
            while (queue.size() == capacity) {
                if (nanos <= 0L) {
                    return false; // Timeout elapsed waiting for space
                }
                nanos = notFull.awaitNanos(nanos);
            }

            queue.add(item);
            // Specifically signal consumer waiting on notEmpty (no thundering herd on producers)
            notEmpty.signal();
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Bounded Take with SLA timeout. Returns null if timeout elapsed without items.
     */
    public T takeWithTimeout(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        if (!lock.tryLock(timeout, unit)) {
            return null;
        }

        try {
            while (queue.isEmpty()) {
                if (nanos <= 0L) {
                    return null; // Timeout elapsed waiting for item
                }
                nanos = notEmpty.awaitNanos(nanos);
            }

            T item = queue.poll();
            // Specifically signal producer waiting on notFull
            notFull.signal();
            return item;
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try {
            return queue.size();
        } finally {
            lock.unlock();
        }
    }
}
