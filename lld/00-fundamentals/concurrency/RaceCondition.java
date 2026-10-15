package conc;

/**
 * The race condition — the disease every production fix cures.
 *
 * count++ is read-modify-write (3 steps). Two threads interleave and lose
 * updates, so the final total is less than the expected 2 * N. Running
 * this a few times gives different (wrong) numbers — the signature of a race.
 */
public class RaceCondition {
    private static int unsafeCount = 0; // shared mutable state, unguarded

    static void demo() throws InterruptedException {
        Runnable task = () -> {
            for (int i = 0; i < 100_000; i++) {
                unsafeCount++; // NOT atomic: read, +1, write
            }
        };
        unsafeCount = 0;
        Thread t1 = new Thread(task);
        Thread t2 = new Thread(task);
        t1.start(); t2.start();
        t1.join(); t2.join();

        System.out.println("Expected 200000, got " + unsafeCount
                + (unsafeCount == 200_000 ? "" : "  <-- lost updates (race)"));
    }
}
