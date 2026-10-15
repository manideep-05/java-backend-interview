package normal;

import java.util.NavigableSet;

/**
 * SCAN / LOOK scheduling: keep moving in the current direction, serving
 * the nearest pending stop ahead; when nothing is ahead, reverse and
 * serve the nearest pending stop in the other direction. This is the
 * "elevator algorithm" and minimizes direction changes.
 */
public class ScanStrategy implements SchedulingStrategy {
    @Override
    public Integer nextTarget(int currentFloor, Direction direction, NavigableSet<Integer> pending) {
        if (pending.isEmpty()) {
            return null;
        }
        if (direction == Direction.UP || direction == Direction.IDLE) {
            // Nearest pending floor >= current (ahead while going up).
            Integer up = pending.ceiling(currentFloor);
            if (up != null) {
                return up;
            }
            // Nothing above: reverse, take the highest below.
            return pending.floor(currentFloor);
        } else { // DOWN
            Integer down = pending.floor(currentFloor);
            if (down != null) {
                return down;
            }
            return pending.ceiling(currentFloor);
        }
    }
}
