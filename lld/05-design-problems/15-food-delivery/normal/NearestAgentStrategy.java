package normal;

import java.util.List;

/** Pick the closest FREE agent (naive scan). */
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
