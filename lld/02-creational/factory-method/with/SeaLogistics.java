package with;

/** A concrete creator: decides to make Ships. */
public class SeaLogistics extends Logistics {
    @Override
    protected Transport createTransport() {
        return new Ship();
    }
}
