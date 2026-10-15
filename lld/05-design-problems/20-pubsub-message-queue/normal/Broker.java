package normal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * In-process pub/sub broker — single-threaded, synchronous.
 *
 * OBSERVER: a topic holds its subscribers; publish fans the message out to
 * all of them. Delivery here is inline with retry-then-DLQ. (The production
 * version makes delivery async per subscriber.)
 */
public class Broker {
    private final Map<String, List<MessageHandler>> topics = new HashMap<>();
    private final List<Message> deadLetterQueue = new ArrayList<>();
    private static final int MAX_ATTEMPTS = 3;

    public void subscribe(String topic, MessageHandler handler) {
        topics.computeIfAbsent(topic, t -> new ArrayList<>()).add(handler);
    }

    public void publish(String topic, Message message) {
        for (MessageHandler handler : topics.getOrDefault(topic, List.of())) {
            deliverWithRetry(handler, message);
        }
    }

    private void deliverWithRetry(MessageHandler handler, Message message) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                handler.handle(message);
                return; // delivered
            } catch (Exception e) {
                System.out.println("delivery attempt " + attempt + " failed: " + e.getMessage());
                if (attempt == MAX_ATTEMPTS) {
                    deadLetterQueue.add(message); // give up -> DLQ
                    System.out.println("dead-lettered " + message);
                }
            }
        }
    }

    public List<Message> getDeadLetterQueue() { return deadLetterQueue; }

    public static void main(String[] args) {
        Broker broker = new Broker();
        broker.subscribe("orders", m -> System.out.println("emailer got " + m));

        // A flaky subscriber that fails the first two attempts of each message.
        int[] fails = {0};
        broker.subscribe("orders", m -> {
            if (fails[0]++ < 2) throw new RuntimeException("transient");
            System.out.println("analytics got " + m);
        });

        broker.publish("orders", new Message("1", "order placed"));
    }
}
