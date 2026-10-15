package normal;

import java.util.ArrayList;
import java.util.List;

/**
 * FACADE orchestrating placement -> assignment -> delivery.
 */
public class OrderService {
    private final List<DeliveryAgent> agents = new ArrayList<>();
    private final AssignmentStrategy assignmentStrategy;

    public OrderService(AssignmentStrategy assignmentStrategy) {
        this.assignmentStrategy = assignmentStrategy;
    }

    public void addAgent(DeliveryAgent agent) {
        agents.add(agent);
    }

    /** When an order is READY, assign a free agent (check-then-claim here). */
    public boolean assignAgent(Order order, int pickupLocation) {
        DeliveryAgent agent = assignmentStrategy.assign(agents, pickupLocation);
        if (agent == null || !agent.claim()) {
            System.out.println("No free agent for order " + order.getId());
            return false;
        }
        order.assign(agent.getId());
        System.out.println("Order " + order.getId() + " assigned to agent " + agent.getId());
        return true;
    }

    public static void main(String[] args) {
        OrderService service = new OrderService(new NearestAgentStrategy());
        service.addAgent(new DeliveryAgent("A1", 2));

        Order order = new Order("O1", "alice");
        order.accept();
        order.ready();
        service.assignAgent(order, 3); // assigned to A1
        order.pickUp();
        order.deliver();
        System.out.println("Final state: " + order.getState());
    }
}
