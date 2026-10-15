package production;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PRODUCTION facade.
 *
 * Concurrency design (same shape as ride matching): optimistic
 * assign-then-CAS-claim with retry. The strategy proposes a free agent;
 * we atomically claim() it. If another order grabbed it first, we
 * re-propose and retry until we claim one or none remain. No global lock,
 * so distinct agents are assigned fully in parallel; the same agent is
 * arbitrated by CAS so it's never double-assigned.
 */
public class OrderService {
    private final List<DeliveryAgent> agents = new CopyOnWriteArrayList<>();
    private final AssignmentStrategy assignmentStrategy;

    public OrderService(AssignmentStrategy assignmentStrategy) {
        this.assignmentStrategy = assignmentStrategy;
    }

    public void addAgent(DeliveryAgent agent) {
        agents.add(agent);
    }

    /** Returns the claimed agent, or null if none could be claimed. */
    public DeliveryAgent assignAgent(int pickupLocation) {
        while (true) {
            DeliveryAgent candidate = assignmentStrategy.assign(agents, pickupLocation);
            if (candidate == null) {
                return null; // no free agents
            }
            if (candidate.claim()) { // atomic FREE -> BUSY
                return candidate;
            }
            // lost the race; try another
        }
    }

    public static void main(String[] args) throws InterruptedException {
        OrderService service = new OrderService(new NearestAgentStrategy());
        service.addAgent(new DeliveryAgent("A1", 5)); // exactly ONE free agent

        AtomicInteger assigned = new AtomicInteger();
        // 7 READY orders race for the single agent; exactly one gets it.
        Runnable order = () -> {
            if (service.assignAgent(5) != null) {
                assigned.incrementAndGet();
            }
        };
        Thread[] ts = new Thread[7];
        for (int i = 0; i < ts.length; i++) {
            ts[i] = new Thread(order, "order-" + i);
            ts[i].start();
        }
        for (Thread t : ts) {
            t.join();
        }
        System.out.println("Orders assigned to the single agent (must be exactly 1): " + assigned.get());
    }
}
