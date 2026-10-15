package with;

/** A concrete decorator: adds milk cost/description on top of whatever it wraps. */
public class MilkDecorator extends BeverageDecorator {

    public MilkDecorator(Beverage wrapped) {
        super(wrapped);
    }

    @Override
    public double cost() {
        return super.cost() + 0.5; // add to the wrapped cost
    }

    @Override
    public String description() {
        return super.description() + " + Milk";
    }
}
