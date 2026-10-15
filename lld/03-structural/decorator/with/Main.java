package with;

/**
 * Features are composed at RUNTIME by stacking wrappers — no explosion
 * of subclasses. Want coffee + double milk + sugar? Just wrap again.
 */
public class Main {
    public static void main(String[] args) {
        Beverage drink = new Coffee();          // Coffee = $2.00
        drink = new MilkDecorator(drink);       // + Milk
        drink = new SugarDecorator(drink);      // + Sugar

        System.out.println(drink.description() + " = $" + drink.cost());
        // Coffee + Milk + Sugar = $2.75

        // Any combination is just a different stack:
        Beverage extraMilk = new MilkDecorator(new MilkDecorator(new Coffee()));
        System.out.println(extraMilk.description() + " = $" + extraMilk.cost());
    }
}
