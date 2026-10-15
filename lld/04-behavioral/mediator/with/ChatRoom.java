package with;

import java.util.ArrayList;
import java.util.List;

/**
 * MEDIATOR APPLIED — the concrete mediator.
 *
 * Holds the users and owns ALL interaction logic (who receives what).
 * Users never reference each other; they only know the mediator. Adding
 * or removing a user is a single register() call.
 */
public class ChatRoom implements ChatMediator {

    private final List<User> users = new ArrayList<>();

    @Override
    public void register(User user) {
        users.add(user);
    }

    @Override
    public void send(String from, String message) {
        for (User user : users) {
            if (!user.getName().equals(from)) { // don't echo back to sender
                user.receive(from, message);
            }
        }
    }
}
