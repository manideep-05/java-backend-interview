package with;

/** Another concrete decorator: adds sugar. */
public class SugarDecorator extends BeverageDecorator {

    public SugarDecorator(Beverage wrapped) {
        super(wrapped);
    }

    @Override
    public double cost() {
        return super.cost() + 0.25;
    }

    @Override
    public String description() {
        return super.description() + " + Sugar";
    }
}
