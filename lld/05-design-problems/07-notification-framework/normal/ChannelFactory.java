package normal;

import java.util.EnumMap;
import java.util.Map;

/**
 * FACTORY: map a Channel type to its concrete NotificationChannel.
 * Centralizes creation so callers never `new` a concrete channel.
 */
public class ChannelFactory {
    private final Map<Channel, NotificationChannel> channels = new EnumMap<>(Channel.class);

    public ChannelFactory() {
        channels.put(Channel.EMAIL, new EmailChannel());
        channels.put(Channel.SMS, new SmsChannel());
        // PUSH could be registered here too.
    }

    public NotificationChannel get(Channel channel) {
        NotificationChannel c = channels.get(channel);
        if (c == null) {
            throw new IllegalArgumentException("No channel registered for " + channel);
        }
        return c;
    }
}
