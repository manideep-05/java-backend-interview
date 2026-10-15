package normal;

/**
 * A scheduled unit of work (COMMAND): a Runnable plus ordering keys.
 * `seq` is a monotonically increasing tiebreaker so tasks with equal
 * runAt keep a stable FIFO order (deterministic scheduling).
 */
public class Task implements Comparable<Task> {
    final Runnable work;
    final long runAtMillis;
    final int priority; // lower = higher priority
    final long seq;

    public Task(Runnable work, long runAtMillis, int priority, long seq) {
        this.work = work;
        this.runAtMillis = runAtMillis;
        this.priority = priority;
        this.seq = seq;
    }

    /** Order by run time, then priority, then submission order. */
    @Override
    public int compareTo(Task other) {
        int byTime = Long.compare(this.runAtMillis, other.runAtMillis);
        if (byTime != 0) return byTime;
        int byPriority = Integer.compare(this.priority, other.priority);
        if (byPriority != 0) return byPriority;
        return Long.compare(this.seq, other.seq);
    }
}
