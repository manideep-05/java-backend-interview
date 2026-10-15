package normal;

import java.util.TreeSet;

/**
 * A single elevator car — single-threaded simulation.
 *
 * Pending stops live in a TreeSet (sorted, deduped) so the SCAN strategy
 * can find the nearest floor in either direction in O(log n). step()
 * advances the car one target at a time.
 */
public class Elevator {
    private final SchedulingStrategy strategy;
    private final TreeSet<Integer> pending = new TreeSet<>();
    private int currentFloor = 0;
    private Direction direction = Direction.IDLE;

    public Elevator(SchedulingStrategy strategy) {
        this.strategy = strategy;
    }

    public void request(Request request) {
        pending.add(request.getFloor());
    }

    /** Process pending stops until none remain. */
    public void run() {
        while (!pending.isEmpty()) {
            Integer target = strategy.nextTarget(currentFloor, direction, pending);
            if (target == null) {
                break;
            }
            direction = target > currentFloor ? Direction.UP
                    : target < currentFloor ? Direction.DOWN : direction;
            moveTo(target);
            pending.remove(target);
            openDoors(target);
        }
        direction = Direction.IDLE;
        System.out.println("Idle at floor " + currentFloor);
    }

    private void moveTo(int target) {
        System.out.println("Moving " + direction + " from " + currentFloor + " to " + target);
        currentFloor = target;
    }

    private void openDoors(int floor) {
        System.out.println("Doors open at floor " + floor);
    }

    public static void main(String[] args) {
        Elevator car = new Elevator(new ScanStrategy());
        car.request(new Request(5, Direction.UP));
        car.request(new Request(3, Direction.UP));
        car.request(new Request(1, Direction.DOWN));
        // From 0 going up: serves 3, then 5, then reverses to 1.
        car.run();
    }
}
