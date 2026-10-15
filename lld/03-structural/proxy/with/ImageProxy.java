package with;

/**
 * PROXY APPLIED — a virtual (lazy-loading) proxy.
 *
 * Implements the same Image interface. It does NOT build the expensive
 * RealImage until display() is first called, and then caches it so
 * repeated calls don't reload. The client sees only an Image.
 */
public class ImageProxy implements Image {

    private final String filename;
    private RealImage realImage; // created lazily, cached after first use

    public ImageProxy(String filename) {
        // Cheap: just remembers the name. No disk load yet.
        this.filename = filename;
    }

    @Override
    public void display() {
        if (realImage == null) {
            realImage = new RealImage(filename); // load on first demand
        }
        realImage.display();
    }
}
