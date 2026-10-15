package production;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PRODUCTION scheduler.
 *
 * Concurrency design:
 *  - A DelayQueue<DelayedTask> is the thread-safe, time-ordered backbone.
 *    Many producers can schedule() concurrently; the queue is internally
 *    synchronized.
 *  - A pool of WORKER THREADS each loop on queue.take(), which BLOCKS
 *    (timed, not spinning) until the earliest task is actually due — so
 *    we never busy-wait and tasks fire at their scheduled time.
 *  - Each run() is wrapped in try/catch so a throwing task is logged and
 *    the worker survives to process the next task (a crash here would
 *    permanently shrink the pool).
 *  - shutdown() flips a flag and interrupts workers for a clean stop.
 */
public class Scheduler {
    private final DelayQueue<DelayedTask> queue = new DelayQueue<>();
    private final Thread[] workers;
    private volatile boolean running = true;

    public Scheduler(int workerCount) {
        this.workers = new Thread[workerCount];
        for (int i = 0; i < workerCount; i++) {
            workers[i] = new Thread(this::workerLoop, "sched-worker-" + i);
            workers[i].start();
        }
    }

    public void schedule(Runnable work, long delayMillis, int priority) {
        long runAt = System.currentTimeMillis() + delayMillis;
        queue.put(new DelayedTask(work, runAt, priority)); // thread-safe enqueue
    }

    private void workerLoop() {
        while (running) {
            try {
                DelayedTask task = queue.take(); // blocks until a task is due
                try {
                    task.work.run();
                } catch (Exception e) {
                    // Isolate task failures so the worker keeps running.
                    System.out.println("Task failed: " + e.getMessage());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return; // shutdown signal
            }
        }
    }

    public void shutdown() {
        running = false;
        for (Thread w : workers) {
            w.interrupt(); // unblock workers parked in take()
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Scheduler scheduler = new Scheduler(3);
        AtomicInteger done = new AtomicInteger();

        // Schedule tasks with mixed delays/priorities from the main thread.
        scheduler.schedule(() -> { done.incrementAndGet(); System.out.println("A (50ms, prio 1)"); }, 50, 1);
        scheduler.schedule(() -> { done.incrementAndGet(); System.out.println("B (now, prio 5)"); }, 0, 5);
        scheduler.schedule(() -> { throw new RuntimeException("boom"); }, 10, 1); // isolated failure
        scheduler.schedule(() -> { done.incrementAndGet(); System.out.println("C (now, prio 1)"); }, 0, 1);

        Thread.sleep(200); // let due tasks fire
        scheduler.shutdown();
        System.out.println("Completed tasks: " + done.get() + " (failing task isolated, pool survived)");
    }
}
