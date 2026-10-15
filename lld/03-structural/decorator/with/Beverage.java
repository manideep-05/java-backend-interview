package with;

/**
 * The component interface shared by concrete beverages AND decorators.
 * Because decorators share this type, they can wrap any Beverage —
 * including other decorators (that's what enables stacking).
 */
public interface Beverage {
    double cost();
    String description();
}
