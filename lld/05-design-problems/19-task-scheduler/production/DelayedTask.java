package production;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A task placed in a DelayQueue.
 *
 * Implements Delayed so DelayQueue.take() blocks until the task is due
 * (getDelay <= 0). compareTo orders by run time, then priority, then a
 * global sequence number for stable ordering of equal timestamps.
 */
public class DelayedTask implements Delayed {
    private static final AtomicLong SEQUENCER = new AtomicLong();

    final Runnable work;
    final long runAtMillis;
    final int priority;
    final long seq;

    public DelayedTask(Runnable work, long runAtMillis, int priority) {
        this.work = work;
        this.runAtMillis = runAtMillis;
        this.priority = priority;
        this.seq = SEQUENCER.getAndIncrement();
    }

    @Override
    public long getDelay(TimeUnit unit) {
        long remaining = runAtMillis - System.currentTimeMillis();
        return unit.convert(remaining, TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        DelayedTask other = (DelayedTask) o;
        int byTime = Long.compare(this.runAtMillis, other.runAtMillis);
        if (byTime != 0) return byTime;
        int byPriority = Integer.compare(this.priority, other.priority);
        if (byPriority != 0) return byPriority;
        return Long.compare(this.seq, other.seq);
    }
}
