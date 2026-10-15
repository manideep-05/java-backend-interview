package with;

/** A concrete component: the base beverage with no add-ons. */
public class Coffee implements Beverage {
    @Override
    public double cost() {
        return 2.0;
    }

    @Override
    public String description() {
        return "Coffee";
    }
}
