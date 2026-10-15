package production;

/**
 * DECORATOR: adds retry-with-backoff around ANY NotificationChannel
 * without changing it. Every channel gets retries for free by wrapping.
 *
 * After maxAttempts it "dead-letters" (here just logs). In production
 * you'd push the failed message to a dead-letter queue for later
 * inspection/replay.
 */
public class RetryingChannel implements NotificationChannel {
    private final NotificationChannel delegate;
    private final int maxAttempts;
    private final long baseBackoffMillis;

    public RetryingChannel(NotificationChannel delegate, int maxAttempts, long baseBackoffMillis) {
        this.delegate = delegate;
        this.maxAttempts = maxAttempts;
        this.baseBackoffMillis = baseBackoffMillis;
    }

    @Override
    public void send(String user, String message) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                delegate.send(user, message);
                return; // success
            } catch (Exception e) {
                System.out.println("Attempt " + attempt + " failed: " + e.getMessage());
                if (attempt == maxAttempts) {
                    System.out.println("Dead-lettering message for " + user);
                    return;
                }
                // Exponential backoff: base * 2^(attempt-1).
                try {
                    Thread.sleep(baseBackoffMillis * (1L << (attempt - 1)));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
}
