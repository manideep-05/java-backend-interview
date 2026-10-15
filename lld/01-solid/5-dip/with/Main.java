package with;

/**
 * The "composition root": the one place that picks concrete
 * implementations and wires them in. Business logic stays decoupled.
 */
public class Main {
    public static void main(String[] args) {
        // Swap the injected sender to change behavior — service untouched.
        NotificationService emailService = new NotificationService(new EmailSender());
        emailService.notifyUser("Your order shipped");

        NotificationService smsService = new NotificationService(new SmsSender());
        smsService.notifyUser("Your OTP is 123456");
    }
}
