package without;

/** A low-level detail: knows how to send email. */
public class EmailSender {
    public void sendEmail(String message) {
        System.out.println("Email sent: " + message);
    }
}
