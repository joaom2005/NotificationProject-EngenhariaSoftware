package pt.ipportalegre.estgd.ei.es.projectnotification.sender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import pt.ipportalegre.estgd.ei.es.projectnotification.entity.Notification;
import pt.ipportalegre.estgd.ei.es.projectnotification.entity.NotificationChannel;

/**
 * Delivers a notification by printing it to the application console (log output).
 * It needs no external system, so it never fails and is the simplest possible channel.
 */
@Component
public class ConsoleSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(ConsoleSender.class);

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.CONSOLE;
    }

    @Override
    public void send(Notification notification) {
        log.info("[CONSOLE] To: {} | Subject: {} | Message: {}",
                notification.getRecipients(),
                notification.getSubject(),
                notification.getMessage());
    }
}