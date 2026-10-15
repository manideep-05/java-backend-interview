package without;

/**
 * DIP VIOLATION.
 *
 * This high-level business class depends directly on the low-level
 * concrete EmailSender, and even constructs it itself with `new`.
 *
 * Consequences:
 *   - You cannot send via SMS/Slack without editing this class.
 *   - You cannot unit-test notify() without actually sending email,
 *     because the dependency is hard-wired and not replaceable.
 */
public class NotificationService {

    private final EmailSender emailSender = new EmailSender(); // hard dependency

    public void notify(String message) {
        emailSender.sendEmail(message);
    }

    public static void main(String[] args) {
        new NotificationService().notify("Your order shipped");
    }
}
