package normal;

/**
 * Entry point. Builds the handler chain once and applies a minimum-level
 * filter cheaply before dispatching into the chain.
 */
public class Logger {
    private final LogHandler chain;
    private final LogLevel minLevel;

    public Logger(LogLevel minLevel) {
        this.minLevel = minLevel;
        // Build the CoR chain: Info -> Error (extend with Debug/Warn as needed).
        LogHandler info = new InfoHandler();
        info.setNext(new ErrorHandler());
        this.chain = info;
    }

    public void log(LogLevel level, String message) {
        // Cheap early filter: drop messages below the configured threshold.
        if (level.ordinal() < minLevel.ordinal()) {
            return;
        }
        chain.handle(level, message);
    }

    public static void main(String[] args) {
        Logger logger = new Logger(LogLevel.INFO);
        logger.log(LogLevel.DEBUG, "ignored, below min");
        logger.log(LogLevel.INFO, "server started");
        logger.log(LogLevel.ERROR, "disk full");
    }
}
