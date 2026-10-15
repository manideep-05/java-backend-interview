package production;

import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * PRODUCTION elevator — a producer/consumer design.
 *
 * Concurrency design:
 *  - Riders on many threads call request() (the PRODUCERS).
 *  - A single dedicated car thread runs the processing loop (the CONSUMER).
 *    One consumer means the car's motion state (currentFloor, direction)
 *    is only ever mutated by one thread → no races on that state.
 *  - pending is a ConcurrentSkipListSet: sorted + deduped + thread-safe,
 *    so producers can add while the consumer reads/removes.
 *  - A ReentrantLock + Condition lets the car thread SLEEP when idle and
 *    be woken when a request arrives, instead of busy-spinning (CPU-friendly).
 */
public class Elevator implements Runnable {
    private final SchedulingStrategy strategy;
    private final ConcurrentSkipListSet<Integer> pending = new ConcurrentSkipListSet<>();
    private volatile int currentFloor = 0;
    private volatile Direction direction = Direction.IDLE;
    private volatile boolean running = true;

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition hasWork = lock.newCondition();

    public Elevator(SchedulingStrategy strategy) {
        this.strategy = strategy;
    }

    /** Producer side: enqueue a stop and wake the car thread if sleeping. */
    public void request(int floor) {
        pending.add(floor);
        lock.lock();
        try {
            hasWork.signal(); // wake the consumer waiting in run()
        } finally {
            lock.unlock();
        }
    }

    public void shutdown() {
        running = false;
        lock.lock();
        try {
            hasWork.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /** Consumer side: the single car-processing loop. */
    @Override
    public void run() {
        while (running) {
            // Sleep until there is work (avoids busy-waiting).
            lock.lock();
            try {
                while (running && pending.isEmpty()) {
                    direction = Direction.IDLE;
                    hasWork.await();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } finally {
                lock.unlock();
            }
            if (!running) {
                return;
            }

            Integer target = strategy.nextTarget(currentFloor, direction, pending);
            if (target == null) {
                continue;
            }
            direction = target > currentFloor ? Direction.UP
                    : target < currentFloor ? Direction.DOWN : direction;
            System.out.println("Moving " + direction + " " + currentFloor + " -> " + target);
            currentFloor = target;
            pending.remove(target);
            System.out.println("Doors open at " + target);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Elevator car = new Elevator(new ScanStrategy());
        Thread carThread = new Thread(car, "car");
        carThread.start();

        // Multiple threads submit hall calls concurrently.
        Runnable rider1 = () -> { car.request(5); car.request(3); };
        Runnable rider2 = () -> { car.request(1); car.request(4); };
        Thread r1 = new Thread(rider1), r2 = new Thread(rider2);
        r1.start(); r2.start();
        r1.join(); r2.join();

        Thread.sleep(200); // let the car drain requests
        car.shutdown();
        carThread.join();
        System.out.println("Elevator stopped at floor " + car.currentFloor);
    }
}
