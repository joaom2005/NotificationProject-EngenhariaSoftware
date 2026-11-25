package pt.ipportalegre.estgd.ei.es.projectnotification;

import org.junit.jupiter.api.Test;
import pt.ipportalegre.estgd.ei.es.projectnotification.entity.Notification;
import pt.ipportalegre.estgd.ei.es.projectnotification.entity.NotificationChannel;
import pt.ipportalegre.estgd.ei.es.projectnotification.sender.NotificationSender;
import pt.ipportalegre.estgd.ei.es.projectnotification.sender.NotificationSenderRegistry;
import pt.ipportalegre.estgd.ei.es.projectnotification.sender.PermanentDeliveryException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NotificationSenderRegistryTest {

    private NotificationSender senderFor(NotificationChannel channel) {
        return new NotificationSender() {
            @Override public NotificationChannel channel() { return channel; }
            @Override public void send(Notification notification) { }
        };
    }

    @Test
    void forChannel_returnsTheSenderOfThatChannel() {
        NotificationSender email = senderFor(NotificationChannel.EMAIL);
        NotificationSender console = senderFor(NotificationChannel.CONSOLE);
        NotificationSenderRegistry registry = new NotificationSenderRegistry(List.of(email, console));

        assertSame(email, registry.forChannel(NotificationChannel.EMAIL));
        assertSame(console, registry.forChannel(NotificationChannel.CONSOLE));
    }

    @Test
    void forChannel_withoutSender_throwsPermanentDeliveryException() {
        NotificationSenderRegistry registry = new NotificationSenderRegistry(
                List.of(senderFor(NotificationChannel.EMAIL)));

        assertThrows(PermanentDeliveryException.class,
                () -> registry.forChannel(NotificationChannel.CONSOLE));
    }

    @Test
    void constructor_withTwoSendersForSameChannel_fails() {
        assertThrows(IllegalStateException.class, () -> new NotificationSenderRegistry(
                List.of(senderFor(NotificationChannel.EMAIL), senderFor(NotificationChannel.EMAIL))));
    }
}