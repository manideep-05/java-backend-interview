package production;

import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * PRODUCTION broker.
 *
 * Concurrency design:
 *  - topics: ConcurrentHashMap<topic, CopyOnWriteArraySet<Subscriber>>.
 *    Many publishers/subscribers mutate/read it concurrently; copy-on-write
 *    gives lock-free iteration during publish while subscribe/unsubscribe
 *    stay safe.
 *  - publish() only ENQUEUES to each subscriber's inbox (non-blocking), so
 *    a slow consumer never blocks the publisher — the whole point of async
 *    pub/sub.
 *  - A single shared DLQ (ConcurrentLinkedQueue) collects messages that
 *    exhausted their retries.
 */
public class Broker {
    private final Map<String, Set<Subscriber>> topics = new ConcurrentHashMap<>();
    private final Queue<Message> deadLetterQueue = new ConcurrentLinkedQueue<>();

    public Subscriber subscribe(String topic, String name, MessageHandler handler) {
        Subscriber sub = new Subscriber(name, handler, 1000, 3, deadLetterQueue);
        topics.computeIfAbsent(topic, t -> new CopyOnWriteArraySet<>()).add(sub);
        return sub;
    }

    public void unsubscribe(String topic, Subscriber sub) {
        Set<Subscriber> subs = topics.get(topic);
        if (subs != null) {
            subs.remove(sub);
        }
        sub.shutdown();
    }

    public void publish(String topic, Message message) {
        for (Subscriber sub : topics.getOrDefault(topic, Set.of())) {
            sub.enqueue(message); // non-blocking fan-out
        }
    }

    public Queue<Message> getDeadLetterQueue() { return deadLetterQueue; }

    public void shutdown() {
        topics.values().forEach(set -> set.forEach(Subscriber::shutdown));
    }

    public static void main(String[] args) throws InterruptedException {
        Broker broker = new Broker();

        broker.subscribe("orders", "emailer", m -> System.out.println("emailer got " + m));

        // Flaky subscriber: fails first 2 attempts of the first message.
        int[] fails = {0};
        broker.subscribe("orders", "analytics", m -> {
            if (fails[0]++ < 2) throw new RuntimeException("transient");
            System.out.println("analytics got " + m);
        });

        broker.publish("orders", new Message("1", "order placed"));
        broker.publish("orders", new Message("2", "order shipped"));

        Thread.sleep(300); // let async workers drain
        broker.shutdown();
        System.out.println("DLQ size: " + broker.getDeadLetterQueue().size());
    }
}
