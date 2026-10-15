package oop;

/**
 * Interface vs Abstract class.
 *
 * - Drawable is an INTERFACE: a pure capability ("can be drawn"). A class
 *   could implement many such interfaces.
 * - AbstractShape is an ABSTRACT CLASS: it holds shared STATE (name) and a
 *   skeleton (describe() implemented once), leaving area() abstract.
 *   Use an abstract class when subclasses share code/fields + identity.
 */
public class Shapes {

    interface Drawable {
        void draw(); // capability contract; no state
    }

    abstract static class AbstractShape implements Drawable {
        private final String name;      // shared STATE lives in the abstract class

        protected AbstractShape(String name) { this.name = name; }

        abstract double area();         // subclass must supply

        // Shared skeleton implemented ONCE here (reused by all subclasses).
        String describe() { return name + " with area " + area(); }
    }

    static class Circle extends AbstractShape {
        private final double r;
        Circle(double r) { super("circle"); this.r = r; }
        double area() { return Math.PI * r * r; }
        public void draw() { System.out.println("drawing a circle"); }
    }

    static class Square extends AbstractShape {
        private final double s;
        Square(double s) { super("square"); this.s = s; }
        double area() { return s * s; }
        public void draw() { System.out.println("drawing a square"); }
    }

    static void demo() {
        AbstractShape c = new Circle(2);
        AbstractShape sq = new Square(3);
        System.out.println(c.describe());  // reuses the abstract class skeleton
        System.out.println(sq.describe());
        c.draw();                          // interface capability
    }
}
