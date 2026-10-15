package normal;

public class ErrorHandler extends LogHandler {
    public ErrorHandler() {
        super(LogLevel.ERROR);
    }

    @Override
    protected void write(String message) {
        System.err.println("[ERROR] " + message);
    }
}
