package pt.ipportalegre.estgd.ei.es.projectnotification.service;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import pt.ipportalegre.estgd.ei.es.projectnotification.dto.NotificationRequest;
import pt.ipportalegre.estgd.ei.es.projectnotification.entity.Notification;
import pt.ipportalegre.estgd.ei.es.projectnotification.repository.NotificationRepository;
import pt.ipportalegre.estgd.ei.es.projectnotification.sender.NotificationSender;

import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationUseCases {

    private final NotificationRepository notificationRepository;
    private final NotificationSender sender;

    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            NotificationSender sender) {
        this.notificationRepository = notificationRepository;
        this.sender = sender;
    }

    @Override
    public List<Notification> getNotifications() {
        return notificationRepository.findAll();
    }

    @Override
    public List<Notification> getSortedNotifications() {
        return notificationRepository.findAll(Sort.by("createdAt").descending());
    }

    @Override
    public Notification getNotification(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found: " + id));
    }

    @Override
    public void createNotification(NotificationRequest request) {
        Notification notification = notificationRepository.save(new Notification(
                request.getRecipients(),
                request.getSubject(),
                request.getMessage()));
        sender.send(notification);
    }
}