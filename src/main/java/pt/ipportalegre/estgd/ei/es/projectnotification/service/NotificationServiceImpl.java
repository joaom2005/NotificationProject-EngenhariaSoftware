package pt.ipportalegre.estgd.ei.es.projectnotification.service;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import pt.ipportalegre.estgd.ei.es.projectnotification.dto.NotificationRequest;
import pt.ipportalegre.estgd.ei.es.projectnotification.entity.Notification;
import pt.ipportalegre.estgd.ei.es.projectnotification.repository.NotificationRepository;

import java.util.List;

/**
 * Handles notification requests. It does NOT deliver anything: it only stores the
 * notification as PENDING. Delivery is picked up later by the delivery component.
 */
@Service
public class NotificationServiceImpl implements NotificationUseCases {

    private final NotificationRepository notificationRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
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
        // A new Notification starts as PENDING, so saving it is enough to queue it
        notificationRepository.save(new Notification(
                request.getRecipients(),
                request.getSubject(),
                request.getMessage()));
    }
}
