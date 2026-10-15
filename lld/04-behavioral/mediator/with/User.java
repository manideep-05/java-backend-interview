package with;

/**
 * A colleague. It knows ONLY the mediator, never other users. To
 * broadcast, it hands the message to the mediator.
 */
public class User {

    private final String name;
    private final ChatMediator mediator;

    public User(String name, ChatMediator mediator) {
        this.name = name;
        this.mediator = mediator;
    }

    public String getName() {
        return name;
    }

    public void send(String message) {
        mediator.send(name, message); // delegate coordination to the mediator
    }

    public void receive(String from, String message) {
        System.out.println("[" + name + "] " + from + ": " + message);
    }
}
