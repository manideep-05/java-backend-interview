package conc;

/**
 * Deadlock — cause and fix.
 *
 * CAUSE: two threads grab two locks in OPPOSITE orders. Thread 1 holds A
 * waits for B; thread 2 holds B waits for A; both wait forever.
 *
 * FIX shown here: a global LOCK ORDERING — every thread always acquires
 * locks in the same order (A then B), which makes the circular wait
 * impossible. (Alternative: tryLock with timeout + backoff.)
 */
public class Deadlock {
    private static final Object A = new Object();
    private static final Object B = new Object();

    // The safe version: both threads lock A then B (consistent order).
    private static void workOnBoth(String who) {
        synchronized (A) {
            synchronized (B) {
                System.out.println(who + " acquired A then B, doing work");
            }
        }
    }

    static void demo() throws InterruptedException {
        // Both threads use the SAME ordering (A -> B), so no deadlock.
        Thread t1 = new Thread(() -> workOnBoth("t1"));
        Thread t2 = new Thread(() -> workOnBoth("t2"));
        t1.start(); t2.start();
        t1.join(); t2.join();
        System.out.println("No deadlock: consistent lock ordering (A before B).");
        // The DANGEROUS version would be: t2 locks B then A -> potential deadlock.
    }
}
