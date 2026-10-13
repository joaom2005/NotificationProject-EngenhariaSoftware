package pt.ipportalegre.estgd.ei.es.projectnotification.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pt.ipportalegre.estgd.ei.es.projectnotification.dto.NotificationRequest;
import pt.ipportalegre.estgd.ei.es.projectnotification.email.EmailSender;
import pt.ipportalegre.estgd.ei.es.projectnotification.entity.Notification;
import pt.ipportalegre.estgd.ei.es.projectnotification.entity.Recipient;
import pt.ipportalegre.estgd.ei.es.projectnotification.repository.NotificationRepository;

import java.util.List;

@Service
public class NotificationServiceImpl implements INotificationUseCases {

    private final NotificationRepository notificationRepository;
    private final EmailSender emailSender;

    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            EmailSender emailSender) {
        this.notificationRepository = notificationRepository;
        this.emailSender = emailSender;
    }

    @Override
    public List<Notification> getNotifications() {
        return notificationRepository.findAll();
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
        emailSender.send(notification);
    }
}