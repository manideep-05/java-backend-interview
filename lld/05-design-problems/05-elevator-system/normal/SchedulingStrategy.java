package normal;

import java.util.NavigableSet;

/**
 * STRATEGY: choose the next target floor given the car's current floor,
 * direction, and the set of pending stops. Swapping SCAN for FCFS is a
 * new implementation, not an edit to the elevator (OCP).
 *
 * Returns Integer (nullable) so "no next target" is representable.
 */
public interface SchedulingStrategy {
    Integer nextTarget(int currentFloor, Direction direction, NavigableSet<Integer> pending);
}
