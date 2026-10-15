package normal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FACADE + Observer-style fan-out (synchronous).
 *
 * Resolves the user's enabled channels (preferences) and dispatches the
 * message to each via its Strategy. Adding a channel type requires no
 * change here.
 */
public class NotificationService {
    private final ChannelFactory factory = new ChannelFactory();
    private final Map<String, List<Channel>> preferences = new HashMap<>();

    public void setPreferences(String user, List<Channel> channels) {
        preferences.put(user, channels);
    }

    public void notify(String user, String message) {
        List<Channel> enabled = preferences.getOrDefault(user, List.of(Channel.EMAIL));
        for (Channel channel : enabled) {
            factory.get(channel).send(user, message); // fan-out
        }
    }

    public static void main(String[] args) {
        NotificationService service = new NotificationService();
        service.setPreferences("alice", List.of(Channel.EMAIL, Channel.SMS));
        service.notify("alice", "Your order shipped");
        // Delivers over both EMAIL and SMS.
    }
}
