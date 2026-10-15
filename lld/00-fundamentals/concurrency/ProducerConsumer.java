package conc;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Producer/consumer with a BlockingQueue — no manual wait/notify.
 *
 * put() blocks when the (bounded) queue is full; take() blocks when empty.
 * A "poison pill" sentinel tells the consumer to stop. This is the backbone
 * of the async logging appender and the task scheduler in the design problems.
 */
public class ProducerConsumer {
    private static final Integer POISON = Integer.MIN_VALUE;

    static void demo() throws InterruptedException {
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(5); // bounded => backpressure

        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 10; i++) {
                    queue.put(i);                 // blocks if full
                    System.out.println("produced " + i);
                }
                queue.put(POISON);                // signal end
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread consumer = new Thread(() -> {
            try {
                while (true) {
                    Integer item = queue.take();  // blocks if empty
                    if (item.equals(POISON)) break;
                    System.out.println("            consumed " + item);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        producer.start(); consumer.start();
        producer.join(); consumer.join();
        System.out.println("producer/consumer done");
    }
}
