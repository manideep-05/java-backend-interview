package with;

/** A concrete creator: decides to make Trucks. */
public class RoadLogistics extends Logistics {
    @Override
    protected Transport createTransport() {
        return new Truck();
    }
}
