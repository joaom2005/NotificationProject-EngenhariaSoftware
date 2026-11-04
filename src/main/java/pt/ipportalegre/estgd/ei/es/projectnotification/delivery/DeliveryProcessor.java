package pt.ipportalegre.estgd.ei.es.projectnotification.delivery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import pt.ipportalegre.estgd.ei.es.projectnotification.entity.Notification;
import pt.ipportalegre.estgd.ei.es.projectnotification.entity.NotificationStatus;
import pt.ipportalegre.estgd.ei.es.projectnotification.repository.NotificationRepository;
import pt.ipportalegre.estgd.ei.es.projectnotification.sender.NotificationSender;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Delivers PENDING notifications and applies the retry policy.
 *
 * <p>The notification row is the single source of truth: it is written to the database
 * before any delivery is attempted, and every outcome is written back afterwards, so a
 * crash or a failing sender can never make a notification disappear.
 *
 * <p>The sender is called OUTSIDE any transaction (a slow SMTP server must not hold a
 * database connection). Only the short step that records the outcome is transactional.
 */
@Component
public class DeliveryProcessor {

    private static final Logger log = LoggerFactory.getLogger(DeliveryProcessor.class);

    private final NotificationRepository repository;
    private final NotificationSender sender;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;
    private final int maxAttempts;
    private final Duration retryDelay;

    public DeliveryProcessor(
            NotificationRepository repository,
            NotificationSender sender,
            TransactionTemplate transactionTemplate,
            Clock clock,
            @Value("${app.delivery.max-attempts:5}") int maxAttempts,
            @Value("${app.delivery.retry-delay:30s}") Duration retryDelay) {
        this.repository = repository;
        this.sender = sender;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
        this.maxAttempts = maxAttempts;
        this.retryDelay = retryDelay;
    }

    /**
     * Makes one delivery attempt for every notification that is due.
     * A problem with one notification never stops the others.
     */
    public void processDueNotifications() {
        List<Notification> due = repository.findDue(NotificationStatus.PENDING, LocalDateTime.now(clock));

        for (Notification notification : due) {
            try {
                deliver(notification);
            } catch (RuntimeException e) {
                // Most likely the database failed while recording the outcome.
                // The notification stays PENDING and will be picked up again.
                log.error("Could not process notification {}", notification.getId(), e);
            }
        }
    }

    private void deliver(Notification notification) {
        Long id = notification.getId();
        String error = null;

        try {
            sender.send(notification);
        } catch (RuntimeException e) {
            error = e.getClass().getSimpleName() + ": " + e.getMessage();
            log.warn("Delivery of notification {} failed: {}", id, error);
        }

        String failure = error;
        transactionTemplate.executeWithoutResult(status -> recordOutcome(id, failure));
    }

    /**
     * Stores the result of one attempt and decides the next state.
     * Reloads the notification so it is attached to the current transaction.
     *
     * @param error failure description, or null if the attempt succeeded
     */
    private void recordOutcome(Long id, String error) {
        Notification notification = repository.findById(id).orElse(null);
        if (notification == null || notification.getStatus() != NotificationStatus.PENDING) {
            log.warn("Notification {} is no longer pending, outcome not recorded", id);
            return;
        }

        boolean success = (error == null);
        notification.recordAttempt(success, error);

        if (success) {
            notification.setStatus(NotificationStatus.SENT);
            notification.setNextAttemptAt(null);
        } else if (notification.getAttemptCount() >= maxAttempts) {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setNextAttemptAt(null);
        } else {
            // Stays PENDING; wait a bit longer after each failure (linear backoff)
            Duration delay = retryDelay.multipliedBy(notification.getAttemptCount());
            notification.setNextAttemptAt(LocalDateTime.now(clock).plus(delay));
        }

        repository.save(notification);
    }
}
