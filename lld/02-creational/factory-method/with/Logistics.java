package with;

/**
 * FACTORY METHOD APPLIED — the creator.
 *
 * planDelivery() (the business logic) is written ONCE against the
 * Transport abstraction. The abstract createTransport() is the
 * "factory method": subclasses decide which concrete Transport to make.
 */
public abstract class Logistics {

    // The factory method — overridden by subclasses.
    protected abstract Transport createTransport();

    // Business logic that never names a concrete product.
    public void planDelivery() {
        Transport transport = createTransport();
        transport.deliver();
    }
}
