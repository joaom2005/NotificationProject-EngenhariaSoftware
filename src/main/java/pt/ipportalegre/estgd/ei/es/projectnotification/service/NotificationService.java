package pt.ipportalegre.estgd.ei.es.projectnotification.service;

import pt.ipportalegre.estgd.ei.es.projectnotification.entity.Notification;

import java.util.List;

public interface NotificationService {
    Notification createNotification();
    void sendNotification();
    List<Notification> getNotifications();
}