package conc;

/** Runs every concurrency fundamentals demo. */
public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Race condition ===");
        RaceCondition.demo();

        System.out.println("\n=== Locks (synchronized / ReentrantLock / ReadWriteLock) ===");
        Locks.demo();

        System.out.println("\n=== volatile vs atomic / CAS ===");
        VolatileVsAtomic.demo();

        System.out.println("\n=== ExecutorService ===");
        ExecutorDemo.demo();

        System.out.println("\n=== Producer / Consumer (BlockingQueue) ===");
        ProducerConsumer.demo();

        System.out.println("\n=== Deadlock avoidance (lock ordering) ===");
        Deadlock.demo();
    }
}
