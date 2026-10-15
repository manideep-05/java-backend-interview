package with;

/** DIP APPLIED — a low-level detail that depends on the abstraction. */
public class EmailSender implements MessageSender {
    @Override
    public void send(String message) {
        System.out.println("Email sent: " + message);
    }
}
