package with;

/** The mediator interface: coordinates communication between users. */
public interface ChatMediator {
    void register(User user);
    void send(String from, String message);
}
