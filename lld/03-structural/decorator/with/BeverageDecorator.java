package with;

/**
 * The base decorator.
 *
 * It IS-A Beverage (so it can stand in anywhere a Beverage is used) and
 * HAS-A Beverage (the thing it wraps). By default it delegates to the
 * wrapped beverage; concrete decorators override to add behavior.
 */
public abstract class BeverageDecorator implements Beverage {

    protected final Beverage wrapped;

    protected BeverageDecorator(Beverage wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public double cost() {
        return wrapped.cost();
    }

    @Override
    public String description() {
        return wrapped.description();
    }
}
