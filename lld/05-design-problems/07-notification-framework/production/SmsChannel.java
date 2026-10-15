package production;

public class SmsChannel implements NotificationChannel {
    @Override
    public void send(String user, String message) {
        System.out.println("SMS to " + user + ": " + message);
    }
}
