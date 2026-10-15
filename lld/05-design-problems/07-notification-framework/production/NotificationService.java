package production;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * PRODUCTION service — async, bounded, resilient.
 *
 * Concurrency / resilience design:
 *  - A bounded ThreadPoolExecutor delivers notifications ASYNC so the
 *    caller (e.g. the checkout request) never blocks on network I/O.
 *  - The queue is BOUNDED (ArrayBlockingQueue) + CallerRunsPolicy, which
 *    provides backpressure: if the system is overloaded, the submitting
 *    thread runs the task itself instead of the queue growing without
 *    bound (which would OOM).
 *  - Each channel is wrapped in a RetryingChannel (Decorator) so
 *    transient failures are retried per channel, in isolation — one
 *    channel failing never blocks another.
 *  - preferences is a ConcurrentHashMap for safe concurrent reads/writes.
 */
public class NotificationService {
    private final Map<Channel, NotificationChannel> channels = new ConcurrentHashMap<>();
    private final Map<String, List<Channel>> preferences = new ConcurrentHashMap<>();
    private final ThreadPoolExecutor executor;

    public NotificationService() {
        // Wrap each channel with retry (3 attempts, 10ms base backoff).
        channels.put(Channel.EMAIL, new RetryingChannel(new EmailChannel(), 3, 10));
        channels.put(Channel.SMS, new RetryingChannel(new SmsChannel(), 3, 10));

        this.executor = new ThreadPoolExecutor(
                2, 4, 60, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(100),              // bounded queue
                new ThreadPoolExecutor.CallerRunsPolicy()); // backpressure
    }

    public void setPreferences(String user, List<Channel> enabled) {
        preferences.put(user, enabled);
    }

    public void notify(String user, String message) {
        List<Channel> enabled = preferences.getOrDefault(user, List.of(Channel.EMAIL));
        for (Channel channel : enabled) {
            NotificationChannel c = channels.get(channel);
            // Submit each channel delivery as an independent async task.
            executor.submit(() -> {
                try {
                    c.send(user, message);
                } catch (Exception e) {
                    System.out.println("Delivery failed: " + e.getMessage());
                }
            });
        }
    }

    public void shutdown() throws InterruptedException {
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    public static void main(String[] args) throws InterruptedException {
        NotificationService service = new NotificationService();
        service.setPreferences("alice", List.of(Channel.EMAIL, Channel.SMS));

        service.notify("alice", "Your order shipped");
        // EMAIL fails once then succeeds on retry; SMS succeeds immediately.

        service.shutdown();
        System.out.println("Done (async delivery completed).");
    }
}
