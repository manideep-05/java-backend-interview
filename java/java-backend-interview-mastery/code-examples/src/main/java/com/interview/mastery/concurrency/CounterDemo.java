package com.interview.mastery.concurrency;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Chapter 06 / CON-01: {@code volatile} is NOT enough for a compound operation like {@code count++}.
 *
 * <p>This demonstrates the lost-update race with a plain/volatile long and the correct fix with
 * {@link AtomicLong}. Run {@link #main(String[])} to observe the broken counter undercount.
 */
public final class CounterDemo {

    /** BROKEN: {@code volatile} gives visibility but {@code count++} is read-modify-write (not atomic). */
    static final class BrokenCounter {
        private volatile long count = 0;
        void inc() { count++; }          // lost updates under concurrency
        long get() { return count; }
    }

    /** FIXED: lock-free atomic compare-and-set. */
    static final class AtomicCounter {
        private final AtomicLong count = new AtomicLong();
        void inc() { count.incrementAndGet(); }
        long get() { return count.get(); }
    }

    private CounterDemo() { }

    /** Runs {@code threads} tasks each incrementing {@code perThread} times; returns the final broken count. */
    public static long runBroken(int threads, int perThread) throws InterruptedException {
        BrokenCounter c = new BrokenCounter();
        runConcurrently(threads, perThread, c::inc);
        return c.get();
    }

    /** Runs the same workload with an {@link AtomicCounter}; returns the final (correct) count. */
    public static long runAtomic(int threads, int perThread) throws InterruptedException {
        AtomicCounter c = new AtomicCounter();
        runConcurrently(threads, perThread, c::inc);
        return c.get();
    }

    private static void runConcurrently(int threads, int perThread, Runnable op) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            for (int t = 0; t < threads; t++) {
                pool.submit(() -> {
                    for (int i = 0; i < perThread; i++) {
                        op.run();
                    }
                });
            }
        } finally {
            pool.shutdown();
            if (!pool.awaitTermination(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException("workload did not finish in time");
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int threads = 10;
        int perThread = 100_000;
        long expected = (long) threads * perThread;

        long broken = runBroken(threads, perThread);
        long atomic = runAtomic(threads, perThread);

        System.out.println("expected        = " + expected);
        System.out.println("brokenCounter   = " + broken + "  (usually < expected: lost updates)");
        System.out.println("atomicCounter   = " + atomic + "  (always == expected)");
    }
}
