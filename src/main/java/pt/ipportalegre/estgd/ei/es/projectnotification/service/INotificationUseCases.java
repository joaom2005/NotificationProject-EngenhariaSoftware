package pt.ipportalegre.estgd.ei.es.projectnotification.service;

import pt.ipportalegre.estgd.ei.es.projectnotification.entity.Notification;
import pt.ipportalegre.estgd.ei.es.projectnotification.entity.Recipient;
import pt.ipportalegre.estgd.ei.es.projectnotification.dto.NotificationRequest;

import java.util.List;

public interface INotificationUseCases {
    List<Notification> getNotifications();
    Notification getNotification(Long id);
    List<Recipient> getRecipients();
    void createNotification(NotificationRequest request);
}