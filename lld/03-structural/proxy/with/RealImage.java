package with;

/**
 * The real subject — still expensive to construct, unchanged. The proxy
 * will control WHEN this gets created.
 */
public class RealImage implements Image {

    private final String filename;

    public RealImage(String filename) {
        this.filename = filename;
        loadFromDisk();
    }

    private void loadFromDisk() {
        System.out.println("Loading large image from disk: " + filename);
    }

    @Override
    public void display() {
        System.out.println("Displaying: " + filename);
    }
}
