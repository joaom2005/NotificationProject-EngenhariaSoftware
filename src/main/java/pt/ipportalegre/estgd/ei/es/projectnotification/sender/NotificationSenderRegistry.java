package pt.ipportalegre.estgd.ei.es.projectnotification.sender;

import org.springframework.stereotype.Component;

import pt.ipportalegre.estgd.ei.es.projectnotification.entity.NotificationChannel;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Finds the sender responsible for a channel. Spring injects every NotificationSender
 * it knows, so supporting a new channel means adding a new sender class and nothing else:
 * no existing class has to change (Open/Closed Principle).
 */
@Component
public class NotificationSenderRegistry {

    private final Map<NotificationChannel, NotificationSender> sendersByChannel;

    public NotificationSenderRegistry(List<NotificationSender> senders) {
        // toMap throws on duplicate keys, so two senders for the same channel fail at startup
        this.sendersByChannel = senders.stream()
                .collect(Collectors.toMap(NotificationSender::channel, Function.identity()));
    }

    /**
     * @throws PermanentDeliveryException if no sender handles the channel; retrying cannot fix that
     */
    public NotificationSender forChannel(NotificationChannel channel) {
        NotificationSender sender = sendersByChannel.get(channel);
        if (sender == null) {
            throw new PermanentDeliveryException("No sender registered for channel " + channel);
        }
        return sender;
    }
}