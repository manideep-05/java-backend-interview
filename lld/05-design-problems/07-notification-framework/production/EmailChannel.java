package production;

/** A channel that occasionally "fails" to demonstrate retry. */
public class EmailChannel implements NotificationChannel {
    private int calls = 0;

    @Override
    public void send(String user, String message) throws Exception {
        calls++;
        // Simulate a transient failure on the first attempt.
        if (calls == 1) {
            throw new Exception("transient SMTP error");
        }
        System.out.println("EMAIL to " + user + ": " + message);
    }
}
