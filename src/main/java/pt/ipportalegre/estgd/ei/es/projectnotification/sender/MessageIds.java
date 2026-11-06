package pt.ipportalegre.estgd.ei.es.projectnotification.sender;

import pt.ipportalegre.estgd.ei.es.projectnotification.entity.Notification;

/** Builds the deterministic Message-ID of a notification, so retries reuse the same one. */
public final class MessageIds {

    private MessageIds() {
    }

    public static String forNotification(Notification notification) {
        return "<notification-" + notification.getId() + "@projectnotification.local>";
    }
}