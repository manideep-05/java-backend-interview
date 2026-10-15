package with;

/**
 * SINGLETON APPLIED — initialization-on-demand holder idiom.
 *
 * Why this idiom:
 *   - LAZY: the Holder class is not loaded (and INSTANCE not created)
 *     until getInstance() is first called.
 *   - THREAD-SAFE: class initialization is guaranteed by the JVM to be
 *     atomic and happen-once, so no synchronized block is needed.
 *   - FAST: no locking on the hot path.
 */
public class ConfigManager {

    private String environment;

    // Private constructor prevents external `new ConfigManager()`.
    private ConfigManager() {
        System.out.println("Loading config from disk..."); // happens exactly once
        this.environment = "production";
    }

    // The holder is loaded lazily by the JVM on first access.
    private static class Holder {
        private static final ConfigManager INSTANCE = new ConfigManager();
    }

    public static ConfigManager getInstance() {
        return Holder.INSTANCE;
    }

    public String get(String key) {
        return key.equals("env") ? environment : "unknown";
    }

    public void set(String key, String value) {
        if (key.equals("env")) {
            this.environment = value;
        }
    }

    public static void main(String[] args) {
        ConfigManager a = ConfigManager.getInstance(); // loads once
        ConfigManager b = ConfigManager.getInstance(); // same object, no reload

        a.set("env", "staging");
        System.out.println("a.env = " + a.get("env")); // staging
        System.out.println("b.env = " + b.get("env")); // staging — single source of truth
        System.out.println("Same object? " + (a == b)); // true
    }
}
