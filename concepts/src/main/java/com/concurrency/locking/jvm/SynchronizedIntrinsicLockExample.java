package com.concurrency.locking.jvm;

import java.util.concurrent.TimeUnit;

/**
 * <h1>JVM INTRINSIC LOCKING: {@code synchronized}</h1>
 *
 * <h2>Under the Hood (JVM / HotSpot Mechanics):</h2>
 * <ul>
 *   <li>Every Java object has an object header containing a <b>Mark Word</b> (64-bit on 64-bit JVM).</li>
 *   <li><b>Biased Locking</b> (Historically enabled, deprecated in modern JDKs): Biased towards first thread without CAS.</li>
 *   <li><b>Lightweight Locking (Thin Lock):</b> Thread uses CAS to copy object's Mark Word to its execution frame (Displaced Mark Word).
 *       If contention occurs, thread spins briefly using adaptive spinning.</li>
 *   <li><b>Heavyweight Locking (Fat Lock):</b> Contention exceeds spin threshold; lock inflates to OS-level Mutex (ObjectMonitor).
 *       Threads are moved to wait-set and parked (OS context switch overhead ~1-10 microseconds).</li>
 * </ul>
 *
 * <h2>Production Best Practices:</h2>
 * <ul>
 *   <li><b>Never synchronize on 'this' or public instances:</b> External callers could synchronize on your object, causing unintended deadlocks (Denial of Service).</li>
 *   <li><b>Use a private final lock object:</b> {@code private final Object lock = new Object();} guarantees encapsulation.</li>
 *   <li><b>Keep synchronized blocks as small as possible:</b> Do not do I/O, network calls, or heavy computation inside.</li>
 * </ul>
 *
 * <h2>Pros:</h2>
 * <ul>
 *   <li>Simplest syntax, automatically releases locks even if exceptions are thrown.</li>
 *   <li>Clean reentrancy (a thread holding the lock can re-enter any synchronized block on the same object).</li>
 * </ul>
 *
 * <h2>Cons / Limitations (Why SDE-2s need explicit locks):</h2>
 * <ul>
 *   <li><b>No timeout support:</b> Cannot bail out if lock is held too long; can cause thread pool exhaustion.</li>
 *   <li><b>Non-interruptible:</b> Thread waiting for intrinsic lock cannot be interrupted via {@code Thread.interrupt()}.</li>
 *   <li><b>No fairness guarantee:</b> JVM does not guarantee FIFO order among waiting threads.</li>
 *   <li><b>Single Condition Set:</b> Object has only one wait-set (via {@code wait()}/{@code notifyAll()}).</li>
 * </ul>
 */
public class SynchronizedIntrinsicLockExample {

    // Dedicated private lock to prevent external lock hijacking
    private final Object balanceLock = new Object();
    private long accountBalance = 0;

    public void deposit(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        synchronized (balanceLock) {
            accountBalance += amount;
        }
    }

    public boolean withdraw(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        synchronized (balanceLock) {
            if (accountBalance >= amount) {
                accountBalance -= amount;
                return true;
            }
            return false;
        }
    }

    public long getBalance() {
        synchronized (balanceLock) {
            return accountBalance;
        }
    }

    /**
     * Demonstrates Reentrancy:
     * A thread that already holds 'balanceLock' can call another method protected by 'balanceLock'
     * without deadlocking on itself. The JVM maintains an internal recursion counter.
     */
    public synchronized void transferWithReentrancy(SynchronizedIntrinsicLockExample target, long amount) {
        // Here 'this' is locked by intrinsic lock
        if (this.withdraw(amount)) {
            target.deposit(amount);
        }
    }
}
