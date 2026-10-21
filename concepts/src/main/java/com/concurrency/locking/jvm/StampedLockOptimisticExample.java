package com.concurrency.locking.jvm;

import java.util.concurrent.locks.StampedLock;

/**
 * <h1>JVM OPTIMISTIC LOCK: {@code StampedLock} (Java 8+)</h1>
 *
 * <h2>Why StampedLock was Created:</h2>
 * <p>In {@link java.util.concurrent.locks.ReentrantReadWriteLock}, readers still perform a CAS write on the AQS state.
 * On multi-core CPUs with many concurrent readers, cache coherency invalidation of the state line creates bus traffic.
 * Furthermore, writers suffer from <b>write starvation</b> because continuous readers prevent writers from acquiring the lock.</p>
 *
 * <h2>How Optimistic Read Works (Zero-Locking Fast Path):</h2>
 * <ol>
 *   <li>{@code tryOptimisticRead()} returns a non-zero version stamp WITHOUT acquiring any lock and WITHOUT modifying state!</li>
 *   <li>The reader reads mutable fields into local variables.</li>
 *   <li>The reader checks {@code lock.validate(stamp)}.</li>
 *   <li>If valid: No write occurred during the read window! Return the local variables safely.</li>
 *   <li>If invalid: A writer stepped in. <b>Fall back to pessimistic {@code readLock()}</b>.</li>
 * </ol>
 *
 * <h2>CRITICAL SDE-2 WARNINGS & GOTCHAS:</h2>
 * <ul>
 *   <li><b>NON-REENTRANT:</b> StampedLock is NOT reentrant! If a thread already holding a write lock attempts to acquire
 *       another write lock, it will <b>DEADLOCK ON ITSELF</b>.</li>
 *   <li><b>NO CONDITIONS:</b> Does not support {@code newCondition()}.</li>
 *   <li><b>DO NOT INTERRUPT:</b> Interrupting a thread waiting in {@code StampedLock.writeLock()} or {@code readLock()}
 *       can cause high CPU spin loop bugs. Always use {@code readLockInterruptibly()} / {@code writeLockInterruptibly()} if interruption is needed.</li>
 * </ul>
 */
public class StampedLockOptimisticExample {

    private double x;
    private double y;
    private final StampedLock stampedLock = new StampedLock();

    public StampedLockOptimisticExample(double initialX, double initialY) {
        this.x = initialX;
        this.y = initialY;
    }

    /**
     * Exclusive Write Lock (Mutually exclusive with all reads and writes).
     */
    public void move(double deltaX, double deltaY) {
        long stamp = stampedLock.writeLock();
        try {
            this.x += deltaX;
            this.y += deltaY;
        } finally {
            stampedLock.unlockWrite(stamp);
        }
    }

    /**
     * Ultra-fast Optimistic Read.
     * Computes distance from origin without blocking incoming writers.
     */
    public double distanceFromOrigin() {
        // Step 1: Attempt optimistic read stamp (Non-blocking, no lock acquired)
        long stamp = stampedLock.tryOptimisticRead();

        // Step 2: Read shared state into thread-local variables
        double currentX = this.x;
        double currentY = this.y;

        // Step 3: Validate if a writer changed state between step 1 and step 2
        if (!stampedLock.validate(stamp)) {
            // Step 4: Fallback to Pessimistic Read Lock if validation failed
            stamp = stampedLock.readLock();
            try {
                currentX = this.x;
                currentY = this.y;
            } finally {
                stampedLock.unlockRead(stamp);
            }
        }

        return Math.sqrt(currentX * currentX + currentY * currentY);
    }

    /**
     * Conditional Write: Move only if at origin (Upgrade from Read to Write).
     */
    public void moveIfAtOrigin(double newX, double newY) {
        long stamp = stampedLock.readLock();
        try {
            while (x == 0.0 && y == 0.0) {
                // Attempt to upgrade read lock to write lock
                long writeStamp = stampedLock.tryConvertToWriteLock(stamp);
                if (writeStamp != 0L) {
                    stamp = writeStamp;
                    x = newX;
                    y = newY;
                    break;
                } else {
                    // Upgrade failed, release read lock and acquire explicit write lock
                    stampedLock.unlockRead(stamp);
                    stamp = stampedLock.writeLock();
                }
            }
        } finally {
            stampedLock.unlock(stamp);
        }
    }
}
