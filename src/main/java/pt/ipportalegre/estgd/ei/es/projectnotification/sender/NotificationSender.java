package pt.ipportalegre.estgd.ei.es.projectnotification.sender;

import pt.ipportalegre.estgd.ei.es.projectnotification.entity.Notification;
import pt.ipportalegre.estgd.ei.es.projectnotification.entity.NotificationChannel;

/**
 * A delivery mechanism for notifications (email today, other channels later).
 */
public interface NotificationSender {
    /** The channel this sender is responsible for. */
    NotificationChannel channel();
    void send(Notification notification);
}