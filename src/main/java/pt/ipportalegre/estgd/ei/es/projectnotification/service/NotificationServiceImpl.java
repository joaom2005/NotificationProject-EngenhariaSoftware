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
import pt.ipportalegre.estgd.ei.es.projectnotification.repository.RecipientRepository;

import java.util.List;

@Service
public class NotificationServiceImpl implements INotificationUseCases {

    private final NotificationRepository notificationRepository;
    private final RecipientRepository recipientRepository;
    private final EmailSender emailSender;

    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            RecipientRepository recipientRepository,
            EmailSender emailSender) {
        this.notificationRepository = notificationRepository;
        this.recipientRepository = recipientRepository;
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
    public List<Recipient> getRecipients() {
        return recipientRepository.findAll();
    }

    @Override
    public void createNotification(NotificationRequest request) {
        if (request.getRecipientIds() == null || request.getRecipientIds().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select at least one recipient");
        }

        List<Recipient> recipients = recipientRepository.findAllById(request.getRecipientIds());
        if (recipients.size() != request.getRecipientIds().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more recipients do not exist");
        }

        Notification notification = new Notification(
                recipients,
                request.getSubject(),
                request.getMessage());
        Notification savedNotification = notificationRepository.save(notification);
        emailSender.send(savedNotification);
    }
}