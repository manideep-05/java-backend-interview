package with;

/**
 * DIP APPLIED — the abstraction both sides depend on.
 *
 * High-level NotificationService and low-level senders both reference
 * THIS interface, not each other. This is the "inversion": the arrow
 * of dependency now points at an abstraction.
 */
public interface MessageSender {
    void send(String message);
}
