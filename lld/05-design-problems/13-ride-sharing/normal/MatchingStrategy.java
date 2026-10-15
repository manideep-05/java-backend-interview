package normal;

import java.util.List;

/**
 * STRATEGY: choose a driver for a pickup location. Swapping "nearest"
 * for "highest-rated" is a new implementation, not an edit to the
 * service (OCP).
 */
public interface MatchingStrategy {
    Driver match(List<Driver> drivers, int pickupLocation);
}
