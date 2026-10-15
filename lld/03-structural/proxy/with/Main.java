package with;

/**
 * Creating the proxy is cheap — no disk load happens here. The heavy
 * load is deferred until the first display() call, and skipped entirely
 * if display() is never called.
 */
public class Main {
    public static void main(String[] args) {
        Image image = new ImageProxy("photo.png"); // no load yet
        System.out.println("Proxy created, nothing loaded so far.");

        image.display(); // NOW it loads, then displays
        image.display(); // already loaded — just displays (cached)
    }
}
