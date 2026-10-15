package conc;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * volatile vs atomic — a common interview trap.
 *
 * - volatile gives VISIBILITY (no stale reads) but NOT atomicity. A
 *   volatile count++ still loses updates because ++ is read-modify-write.
 * - AtomicInteger.incrementAndGet() is a single atomic CAS operation, so
 *   it is both visible AND atomic.
 *
 * Takeaway: use volatile for flags; use atomics for counters/accumulators.
 */
public class VolatileVsAtomic {
    private static volatile int volatileCount = 0;       // visible, NOT atomic
    private static final AtomicInteger atomicCount = new AtomicInteger(); // atomic

    static void demo() throws InterruptedException {
        Runnable volatileTask = () -> { for (int i = 0; i < 100_000; i++) volatileCount++; };
        Runnable atomicTask = () -> { for (int i = 0; i < 100_000; i++) atomicCount.incrementAndGet(); };

        volatileCount = 0;
        Thread a = new Thread(volatileTask), b = new Thread(volatileTask);
        a.start(); b.start(); a.join(); b.join();
        System.out.println("volatile count (want 200000): " + volatileCount + "  (likely wrong)");

        atomicCount.set(0);
        Thread c = new Thread(atomicTask), d = new Thread(atomicTask);
        c.start(); d.start(); c.join(); d.join();
        System.out.println("atomic count   (want 200000): " + atomicCount.get() + "  (always correct)");

        // CAS directly: set to 100 only if currently 0.
        AtomicInteger slot = new AtomicInteger(0);
        boolean won = slot.compareAndSet(0, 100);
        System.out.println("CAS 0->100 succeeded: " + won + ", value now " + slot.get());
    }
}
