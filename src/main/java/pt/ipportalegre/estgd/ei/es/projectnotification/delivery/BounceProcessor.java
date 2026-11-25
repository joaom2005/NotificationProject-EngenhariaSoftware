package pt.ipportalegre.estgd.ei.es.projectnotification.delivery;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import pt.ipportalegre.estgd.ei.es.projectnotification.entity.Notification;
import pt.ipportalegre.estgd.ei.es.projectnotification.entity.NotificationStatus;
import pt.ipportalegre.estgd.ei.es.projectnotification.repository.NotificationRepository;
import pt.ipportalegre.estgd.ei.es.projectnotification.sender.BounceDetector;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class BounceProcessor {

    private final NotificationRepository repository;
    private final BounceDetector bounceDetector;
    private final TransactionTemplate transactionTemplate;
    private final Duration window;

    public BounceProcessor(
            NotificationRepository repository,
            BounceDetector bounceDetector,
            PlatformTransactionManager transactionManager,
            @Value("${app.delivery.bounce.window:24h}") String window) {
        this.repository = repository;
        this.bounceDetector = bounceDetector;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.window = DurationStyle.detectAndParse(window);
    }

    public void processRecentBounces() {
        LocalDateTime since = LocalDateTime.now().minus(window);

        var notifications = repository.findByStatusAndSentAtAfterOrderBySentAtAsc(
                NotificationStatus.SENT, since);

        for (Notification notification : notifications) {
            if (!"EMAIL".equals(notification.getChannel().name())) {
                continue;
            }

            bounceDetector.findBounce(notification)
                    .ifPresent(reason -> recordBounce(notification.getId(), reason));
        }
    }

    private void recordBounce(Long notificationId, String reason) {
        transactionTemplate.executeWithoutResult(transaction -> {
            repository.findLockedById(notificationId).ifPresent(notification -> {
                if (notification.getStatus() == NotificationStatus.SENT) {
                    notification.markBounced(reason);
                }
            });
        });
    }
}