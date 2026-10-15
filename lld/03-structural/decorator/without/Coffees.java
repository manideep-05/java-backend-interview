package without;

/**
 * DECORATOR NOT USED — the subclass-explosion problem.
 *
 * Every combination of add-ons needs its own class. With 3 add-ons you
 * already need 2^3 = 8 classes, duplicating cost/description logic.
 * Adding "caramel" would double the class count again.
 */
public class Coffees {

    static class Coffee {
        public double cost() { return 2.0; }
        public String description() { return "Coffee"; }
    }

    static class CoffeeWithMilk extends Coffee {
        @Override public double cost() { return super.cost() + 0.5; }
        @Override public String description() { return super.description() + " + Milk"; }
    }

    static class CoffeeWithMilkAndSugar extends CoffeeWithMilk {
        @Override public double cost() { return super.cost() + 0.25; }
        @Override public String description() { return super.description() + " + Sugar"; }
    }
    // ...and CoffeeWithSugar, CoffeeWithSugarAndMilk, etc. — explosion.

    public static void main(String[] args) {
        Coffee c = new CoffeeWithMilkAndSugar();
        System.out.println(c.description() + " = $" + c.cost());
    }
}
