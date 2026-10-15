package production;

import java.util.NavigableSet;

/** STRATEGY: pick the next target floor (same contract as normal). */
public interface SchedulingStrategy {
    Integer nextTarget(int currentFloor, Direction direction, NavigableSet<Integer> pending);
}
