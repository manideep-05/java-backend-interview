package production;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * DECORATOR + producer/consumer: wraps a real appender and makes writes
 * ASYNC so the logging call on the hot path is just an enqueue.
 *
 * Concurrency design:
 *  - A BOUNDED LinkedBlockingQueue holds pending lines. Bounded so a
 *    flood of logs can't exhaust memory. If full, offer() returns false
 *    and we DROP the line (logging should degrade, not crash the app) —
 *    a deliberate, documented policy choice.
 *  - A single dedicated worker thread drains the queue and performs the
 *    real (slow) write via the delegate. One consumer keeps output ordered.
 *  - close() drains remaining lines and joins the worker (flush on
 *    shutdown) so we don't silently lose buffered logs on a clean stop.
 */
public class AsyncAppender implements Appender {
    private final Appender delegate;
    private final BlockingQueue<String> queue;
    private final Thread worker;
    private volatile boolean running = true;

    public AsyncAppender(Appender delegate, int capacity) {
        this.delegate = delegate;
        this.queue = new LinkedBlockingQueue<>(capacity);
        this.worker = new Thread(this::drain, "log-writer");
        this.worker.start();
    }

    @Override
    public void append(String line) {
        // Non-blocking enqueue; drop if the buffer is full (shed load).
        if (!queue.offer(line)) {
            // In practice: increment a "dropped logs" metric here.
        }
    }

    private void drain() {
        try {
            while (running || !queue.isEmpty()) {
                String line = queue.poll(100, java.util.concurrent.TimeUnit.MILLISECONDS);
                if (line != null) {
                    delegate.append(line); // the slow I/O, off the hot path
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Flush-on-shutdown: stop accepting, drain the rest, join the worker. */
    public void close() throws InterruptedException {
        running = false;
        worker.join();
    }
}
