package normal;

/**
 * CHAIN OF RESPONSIBILITY base handler.
 *
 * Each handler is responsible for ONE level. If the message matches its
 * level it writes it; regardless, it forwards to the next handler so a
 * message can be processed by multiple handlers (or just the matching one).
 */
public abstract class LogHandler {
    protected final LogLevel level;
    private LogHandler next;

    protected LogHandler(LogLevel level) {
        this.level = level;
    }

    public LogHandler setNext(LogHandler next) {
        this.next = next;
        return next; // return next to allow fluent chaining
    }

    public void handle(LogLevel messageLevel, String message) {
        if (messageLevel == level) {
            write(message);
        }
        if (next != null) {
            next.handle(messageLevel, message); // pass along the chain
        }
    }

    protected abstract void write(String message);
}
