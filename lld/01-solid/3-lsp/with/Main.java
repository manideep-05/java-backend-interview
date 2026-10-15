package with;

/**
 * Now any Shape is a safe substitute for any other Shape in code that
 * only relies on the Shape contract (getArea). No instanceof checks,
 * no broken assumptions.
 */
public class Main {

    static void printArea(Shape shape) {
        System.out.println("Area = " + shape.getArea());
    }

    public static void main(String[] args) {
        printArea(new Rectangle(5, 10)); // 50
        printArea(new Square(5));        // 25 — correct and expected
    }
}
