package pt.ipportalegre.estgd.ei.es.projectnotification.sender;

import pt.ipportalegre.estgd.ei.es.projectnotification.entity.Notification;

/**
 * A delivery mechanism for notifications (email today, other channels later).
 */
public interface NotificationSender {

    void send(Notification notification);
}