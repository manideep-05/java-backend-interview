package normal;

import java.util.List;

/** STRATEGY: choose a delivery agent for a pickup location. */
public interface AssignmentStrategy {
    DeliveryAgent assign(List<DeliveryAgent> agents, int pickupLocation);
}
