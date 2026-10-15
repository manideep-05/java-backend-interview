package normal;

/**
 * A request to stop at a floor. Hall calls carry a desired direction;
 * car-button requests can use UP/DOWN based on target vs current, or we
 * simply treat the target floor as the key.
 */
public class Request {
    private final int floor;
    private final Direction direction;

    public Request(int floor, Direction direction) {
        this.floor = floor;
        this.direction = direction;
    }

    public int getFloor() {
        return floor;
    }

    public Direction getDirection() {
        return direction;
    }
}
