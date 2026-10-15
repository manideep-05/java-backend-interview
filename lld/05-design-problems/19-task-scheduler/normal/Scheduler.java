package normal;

import java.util.PriorityQueue;

/**
 * Single-threaded scheduler.
 *
 * A PriorityQueue orders tasks by (runAt, priority, seq). runPending()
 * drains tasks that are due. In this simple version we run everything in
 * submission/priority order; the production version adds real timed
 * waiting and a worker pool.
 */
public class Scheduler {
    private final PriorityQueue<Task> queue = new PriorityQueue<>();
    private long seq = 0;

    public void schedule(Runnable work, long delayMillis, int priority) {
        long runAt = System.currentTimeMillis() + delayMillis;
        queue.add(new Task(work, runAt, priority, seq++));
    }

    /** Run all currently-due tasks in order (earliest/highest priority first). */
    public void runPending() {
        long now = System.currentTimeMillis();
        while (!queue.isEmpty() && queue.peek().runAtMillis <= now) {
            Task task = queue.poll();
            try {
                task.work.run();
            } catch (Exception e) {
                // Isolate failures: one bad task must not stop the loop.
                System.out.println("Task failed: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        Scheduler scheduler = new Scheduler();
        scheduler.schedule(() -> System.out.println("low priority"), 0, 10);
        scheduler.schedule(() -> System.out.println("high priority"), 0, 1);
        scheduler.schedule(() -> { throw new RuntimeException("boom"); }, 0, 5);

        scheduler.runPending();
        // Prints high priority, then the failing task is isolated, then low priority.
    }
}
