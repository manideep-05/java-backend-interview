package without;

/**
 * FACTORY METHOD NOT USED — the problem.
 *
 * Creation is a hard-coded if/else on a string. Every planning method
 * that needs a transport repeats this branching, and adding a new mode
 * (air) means hunting down and editing every such block.
 */
public class LogisticsApp {

    public void planDelivery(String type) {
        // Creation logic welded into business logic.
        if (type.equals("road")) {
            System.out.println("Deliver by Truck on the highway");
        } else if (type.equals("sea")) {
            System.out.println("Deliver by Ship across the ocean");
        } else {
            throw new IllegalArgumentException("Unknown type: " + type);
        }
    }

    public static void main(String[] args) {
        LogisticsApp app = new LogisticsApp();
        app.planDelivery("road");
        app.planDelivery("sea");
    }
}
