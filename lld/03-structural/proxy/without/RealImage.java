package without;

/**
 * PROXY NOT USED — the problem.
 *
 * The heavy load happens in the constructor. So just CREATING the image
 * pays the full cost, even if the program never actually displays it.
 */
public class RealImage {

    private final String filename;

    public RealImage(String filename) {
        this.filename = filename;
        loadFromDisk(); // expensive, runs eagerly on construction
    }

    private void loadFromDisk() {
        System.out.println("Loading large image from disk: " + filename);
    }

    public void display() {
        System.out.println("Displaying: " + filename);
    }

    public static void main(String[] args) {
        // Loads immediately even though we may never display it.
        RealImage image = new RealImage("photo.png");
        // ... if we never call display(), the load was wasted.
        image.display();
    }
}
