package production;

import java.util.List;

/** STRATEGY: choose a delivery agent (same contract as normal). */
public interface AssignmentStrategy {
    DeliveryAgent assign(List<DeliveryAgent> agents, int pickupLocation);
}
