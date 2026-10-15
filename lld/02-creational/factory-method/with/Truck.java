package with;

/** A concrete product. */
public class Truck implements Transport {
    @Override
    public void deliver() {
        System.out.println("Deliver by Truck on the highway");
    }
}
