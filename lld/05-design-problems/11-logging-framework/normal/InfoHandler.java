package normal;

public class InfoHandler extends LogHandler {
    public InfoHandler() {
        super(LogLevel.INFO);
    }

    @Override
    protected void write(String message) {
        System.out.println("[INFO] " + message);
    }
}
