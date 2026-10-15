package production;

/** STRATEGY: a delivery channel. send() may throw on transient failures. */
public interface NotificationChannel {
    void send(String user, String message) throws Exception;
}
