package without;

/**
 * VISITOR NOT USED — the problem.
 *
 * Every new operation (area, then XML export, then JSON export...) must
 * be added as a method to EVERY shape class. The shapes accumulate
 * unrelated responsibilities and each new operation edits every class.
 */
public class Shapes {

    interface Shape {
        double area();
        String exportXml();   // added later
        // String exportJson(); // and this would edit every shape again...
    }

    static class Circle implements Shape {
        double radius;
        Circle(double radius) { this.radius = radius; }
        public double area() { return Math.PI * radius * radius; }
        public String exportXml() { return "<circle r='" + radius + "'/>"; }
    }

    static class Rectangle implements Shape {
        double w, h;
        Rectangle(double w, double h) { this.w = w; this.h = h; }
        public double area() { return w * h; }
        public String exportXml() { return "<rect w='" + w + "' h='" + h + "'/>"; }
    }

    public static void main(String[] args) {
        Shape c = new Circle(2);
        System.out.println(c.area());
        System.out.println(c.exportXml());
    }
}
