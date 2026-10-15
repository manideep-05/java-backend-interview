package with;

/** Another concrete product. */
public class Ship implements Transport {
    @Override
    public void deliver() {
        System.out.println("Deliver by Ship across the ocean");
    }
}
