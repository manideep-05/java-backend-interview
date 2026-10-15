package production;

import java.util.List;

/** STRATEGY: choose a driver for a pickup (same contract as normal). */
public interface MatchingStrategy {
    Driver match(List<Driver> drivers, int pickupLocation);
}
