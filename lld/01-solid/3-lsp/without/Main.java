package without;

/**
 * Demonstrates the broken substitution.
 *
 * The method stretchAndCheck is written against Rectangle. It is
 * correct for every real Rectangle, but a Square (a declared subtype)
 * makes it fail — proving Square is not substitutable.
 */
public class Main {

    // Written assuming the Rectangle contract: setting width = 5 on a
    // rectangle of height 10 must give area 50.
    static void stretchAndCheck(Rectangle r) {
        r.setHeight(10);
        r.setWidth(5);
        int expected = 5 * 10;
        System.out.println("Expected area: " + expected + ", actual: " + r.getArea()
                + (r.getArea() == expected ? "  OK" : "  <-- BROKEN"));
    }

    public static void main(String[] args) {
        stretchAndCheck(new Rectangle()); // OK -> 50
        stretchAndCheck(new Square());    // BROKEN -> 25 (setWidth clobbered height)
    }
}
