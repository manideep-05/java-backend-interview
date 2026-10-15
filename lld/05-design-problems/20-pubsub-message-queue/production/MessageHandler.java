package production;

/** Handler contract; may throw to signal a transient failure. */
public interface MessageHandler {
    void handle(Message message) throws Exception;
}
