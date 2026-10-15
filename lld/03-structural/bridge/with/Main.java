package with;

/**
 * Combinations are composed at runtime. To add "Green" you'd write ONE
 * class implementing Color — every existing shape works with it
 * immediately. That's the N+M (not N*M) payoff.
 */
public class Main {
    public static void main(String[] args) {
        Shape redCircle = new Circle(new Red());
        Shape blueSquare = new Square(new Blue());

        redCircle.draw();  // Drawing Circle in Red
        blueSquare.draw(); // Drawing Square in Blue
    }
}
