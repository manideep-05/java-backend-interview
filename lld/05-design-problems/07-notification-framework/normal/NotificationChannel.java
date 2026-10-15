package normal;

/**
 * STRATEGY: one way to deliver a message. A new channel = a new
 * implementation, no change to the service (OCP, DIP).
 */
public interface NotificationChannel {
    void send(String user, String message);
}
