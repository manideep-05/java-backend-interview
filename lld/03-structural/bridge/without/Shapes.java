package without;

/**
 * BRIDGE NOT USED — the class-explosion problem.
 *
 * Two dimensions (shape, color) are entangled in one inheritance tree,
 * so you need one class per COMBINATION. 2 shapes x 2 colors = 4
 * classes; adding "green" adds a class per shape, and adding "triangle"
 * adds a class per color. This is N x M growth.
 */
public class Shapes {

    static abstract class Shape { abstract void draw(); }

    static class RedCircle extends Shape {
        void draw() { System.out.println("Drawing Circle in Red"); }
    }
    static class BlueCircle extends Shape {
        void draw() { System.out.println("Drawing Circle in Blue"); }
    }
    static class RedSquare extends Shape {
        void draw() { System.out.println("Drawing Square in Red"); }
    }
    static class BlueSquare extends Shape {
        void draw() { System.out.println("Drawing Square in Blue"); }
    }

    public static void main(String[] args) {
        new RedCircle().draw();
        new BlueSquare().draw();
    }
}
