package pt.ipportalegre.estgd.ei.es.projectnotification;

import org.junit.jupiter.api.Test;

import pt.ipportalegre.estgd.ei.es.projectnotification.entity.Notification;
import pt.ipportalegre.estgd.ei.es.projectnotification.entity.NotificationStatus;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NotificationTest {

    private Notification pending() {
        return new Notification(List.of("a@example.com"), "Subject", "Body");
    }

    @Test
    void newNotification_isPending() {
        assertEquals(NotificationStatus.PENDING, pending().getStatus());
    }

    @Test
    void markSent_changesStatusAndClearsNextAttempt() {
        Notification notification = pending();
        notification.scheduleRetry(LocalDateTime.now().plusMinutes(1));

        notification.markSent();

        assertEquals(NotificationStatus.SENT, notification.getStatus());
        assertNull(notification.getNextAttemptAt());
    }

    @Test
    void markFailed_changesStatusAndClearsNextAttempt() {
        Notification notification = pending();

        notification.markFailed();

        assertEquals(NotificationStatus.FAILED, notification.getStatus());
        assertNull(notification.getNextAttemptAt());
    }

    @Test
    void scheduleRetry_keepsPendingAndSetsNextAttempt() {
        Notification notification = pending();
        LocalDateTime when = LocalDateTime.now().plusMinutes(5);

        notification.scheduleRetry(when);

        assertEquals(NotificationStatus.PENDING, notification.getStatus());
        assertEquals(when, notification.getNextAttemptAt());
    }

    @Test
    void sentNotification_cannotChangeStateAnymore() {
        Notification notification = pending();
        notification.markSent();

        assertThrows(IllegalStateException.class, notification::markFailed);
        assertThrows(IllegalStateException.class, notification::markSent);
        assertThrows(IllegalStateException.class,
                () -> notification.scheduleRetry(LocalDateTime.now()));
        assertEquals(NotificationStatus.SENT, notification.getStatus());
    }

    @Test
    void failedNotification_cannotBeRevived() {
        Notification notification = pending();
        notification.markFailed();

        assertThrows(IllegalStateException.class, notification::markSent);
        assertEquals(NotificationStatus.FAILED, notification.getStatus());
    }
}