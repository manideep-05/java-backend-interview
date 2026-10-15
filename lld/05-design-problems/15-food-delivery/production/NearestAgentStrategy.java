package production;

import java.util.List;

/** Nearest FREE agent; stateless => thread-safe. Only proposes; the
 *  atomic claim in the service guarantees exclusivity. */
public class NearestAgentStrategy implements AssignmentStrategy {
    @Override
    public DeliveryAgent assign(List<DeliveryAgent> agents, int pickupLocation) {
        DeliveryAgent best = null;
        int bestDist = Integer.MAX_VALUE;
        for (DeliveryAgent a : agents) {
            if (!a.isBusy()) {
                int dist = Math.abs(a.getLocation() - pickupLocation);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = a;
                }
            }
        }
        return best;
    }
}
