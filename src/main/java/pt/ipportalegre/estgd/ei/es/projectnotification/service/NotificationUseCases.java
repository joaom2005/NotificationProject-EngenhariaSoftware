package pt.ipportalegre.estgd.ei.es.projectnotification.service;

import pt.ipportalegre.estgd.ei.es.projectnotification.entity.Notification;
import pt.ipportalegre.estgd.ei.es.projectnotification.dto.NotificationRequest;

import java.util.List;

public interface NotificationUseCases {
    List<Notification> getNotifications();
    Notification getNotification(Long id);
    void createNotification(NotificationRequest request);
}