package production;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 * PRODUCTION subscriber = its OWN inbox queue + worker thread.
 *
 * Why per-subscriber queue+worker:
 *  - ISOLATION: a slow or failing subscriber only backs up ITS queue; the
 *    publisher and other subscribers are never blocked (the per-consumer-lag fix).
 *  - The inbox is a BOUNDED BlockingQueue -> backpressure (offer drops when full).
 *  - A single worker thread per subscriber preserves per-subscriber message ORDER.
 *  - Retry with exponential backoff; after maxAttempts, route to the shared DLQ.
 */
public class Subscriber {
    private final String name;
    private final MessageHandler handler;
    private final BlockingQueue<Message> inbox;
    private final Queue<Message> deadLetterQueue; // shared broker DLQ
    private final int maxAttempts;
    private final Thread worker;
    private volatile boolean running = true;

    public Subscriber(String name, MessageHandler handler, int capacity,
                      int maxAttempts, Queue<Message> deadLetterQueue) {
        this.name = name;
        this.handler = handler;
        this.inbox = new ArrayBlockingQueue<>(capacity);
        this.maxAttempts = maxAttempts;
        this.deadLetterQueue = deadLetterQueue;
        this.worker = new Thread(this::drain, "sub-" + name);
        this.worker.start();
    }

    /** Non-blocking enqueue; drop if the inbox is full (documented backpressure). */
    public void enqueue(Message m) {
        if (!inbox.offer(m)) {
            System.out.println("[" + name + "] inbox full, dropping " + m);
        }
    }

    private void drain() {
        while (running || !inbox.isEmpty()) {
            try {
                Message m = inbox.poll(100, TimeUnit.MILLISECONDS);
                if (m != null) {
                    deliver(m);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void deliver(Message m) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                handler.handle(m);
                return;
            } catch (Exception e) {
                if (attempt == maxAttempts) {
                    deadLetterQueue.add(m); // give up -> DLQ (don't lose it)
                    System.out.println("[" + name + "] dead-lettered " + m);
                    return;
                }
                sleep(10L * (1L << (attempt - 1))); // exponential backoff
            }
        }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    public void shutdown() {
        running = false;
        worker.interrupt();
    }

    public String getName() { return name; }

    // for the demo's DLQ wiring convenience
    public static Queue<Message> newDlq() { return new ConcurrentLinkedQueue<>(); }
}
