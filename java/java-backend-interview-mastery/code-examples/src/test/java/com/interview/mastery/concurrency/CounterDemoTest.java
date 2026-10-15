package com.interview.mastery.concurrency;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CounterDemoTest {

    private static final int THREADS = 8;
    private static final int PER_THREAD = 50_000;
    private static final long EXPECTED = (long) THREADS * PER_THREAD;

    @Test
    void atomicCounterIsAlwaysCorrect() throws InterruptedException {
        assertEquals(EXPECTED, CounterDemo.runAtomic(THREADS, PER_THREAD),
                "AtomicLong.incrementAndGet must never lose updates");
    }

    @Test
    void brokenCounterNeverExceedsExpected() throws InterruptedException {
        // The volatile counter may undercount due to lost updates; it must never *exceed* expected.
        // We assert the invariant (<= expected) rather than requiring it to be wrong, to avoid a
        // flaky test on platforms where the race happens not to manifest.
        long broken = CounterDemo.runBroken(THREADS, PER_THREAD);
        assertTrue(broken <= EXPECTED, "broken counter should be <= expected, was " + broken);
    }
}
