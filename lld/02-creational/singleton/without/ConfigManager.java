package without;

/**
 * SINGLETON NOT USED — the problem.
 *
 * The constructor is public and does expensive work (loading a config
 * file). Every caller that does `new ConfigManager()` reloads the file
 * and gets its OWN copy of the settings. There is no single source of
 * truth: if one copy updates a value, the others never see it.
 */
public class ConfigManager {

    private String environment;

    public ConfigManager() {
        // Pretend this reads and parses a file from disk every time.
        System.out.println("Loading config from disk...");
        this.environment = "production";
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
        ConfigManager a = new ConfigManager(); // loads from disk
        ConfigManager b = new ConfigManager(); // loads AGAIN, separate object

        a.set("env", "staging");
        // b never saw a's change — different objects.
        System.out.println("a.env = " + a.get("env")); // staging
        System.out.println("b.env = " + b.get("env")); // production
        System.out.println("Same object? " + (a == b)); // false
    }
}
