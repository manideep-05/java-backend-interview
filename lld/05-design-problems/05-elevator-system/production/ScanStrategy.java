package production;

import java.util.NavigableSet;

/** SCAN/LOOK scheduling (stateless => safe to share across threads). */
public class ScanStrategy implements SchedulingStrategy {
    @Override
    public Integer nextTarget(int currentFloor, Direction direction, NavigableSet<Integer> pending) {
        if (pending.isEmpty()) {
            return null;
        }
        if (direction == Direction.UP || direction == Direction.IDLE) {
            Integer up = pending.ceiling(currentFloor);
            return up != null ? up : pending.floor(currentFloor);
        } else {
            Integer down = pending.floor(currentFloor);
            return down != null ? down : pending.ceiling(currentFloor);
        }
    }
}
