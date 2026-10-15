package with;

/**
 * DIP APPLIED — a NEW low-level detail.
 *
 * Added SMS support with zero changes to NotificationService,
 * because the service depends only on MessageSender.
 */
public class SmsSender implements MessageSender {
    @Override
    public void send(String message) {
        System.out.println("SMS sent: " + message);
    }
}
