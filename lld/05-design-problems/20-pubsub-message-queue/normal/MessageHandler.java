package normal;

/**
 * What a subscriber does with a message. May throw to signal a transient
 * failure (the broker will retry, then dead-letter).
 */
public interface MessageHandler {
    void handle(Message message) throws Exception;
}
