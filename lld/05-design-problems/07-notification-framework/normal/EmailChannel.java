package normal;

public class EmailChannel implements NotificationChannel {
    @Override
    public void send(String user, String message) {
        System.out.println("EMAIL to " + user + ": " + message);
    }
}
