package with;

/**
 * DIP APPLIED.
 *
 * The high-level policy now depends only on the MessageSender
 * abstraction. The concrete sender is INJECTED through the
 * constructor (Dependency Injection), so:
 *   - Any sender (email, SMS, a test fake) works with no edits here.
 *   - notify() is trivially unit-testable with a mock MessageSender.
 */
public class NotificationService {

    private final MessageSender sender;

    // The dependency is provided from outside, not created inside.
    public NotificationService(MessageSender sender) {
        this.sender = sender;
    }

    public void notifyUser(String message) {
        sender.send(message);
    }
}
