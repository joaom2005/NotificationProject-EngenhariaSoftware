package pt.ipportalegre.estgd.ei.es.projectnotification.delivery;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import pt.ipportalegre.estgd.ei.es.projectnotification.entity.Notification;
import pt.ipportalegre.estgd.ei.es.projectnotification.entity.NotificationPriority;
import pt.ipportalegre.estgd.ei.es.projectnotification.entity.NotificationStatus;
import pt.ipportalegre.estgd.ei.es.projectnotification.repository.NotificationRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class NotificationClaimer {

    private final NotificationRepository repository;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;
    private final PriorityPolicy policy;
    private final Duration claimDuration;

    public NotificationClaimer(
            NotificationRepository repository,
            TransactionTemplate transactionTemplate,
            Clock clock,
            PriorityPolicy policy,
            @Value("${app.delivery.claim-duration:5m}") String claimDuration) {
        this.repository = repository;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
        this.policy = policy;
        this.claimDuration = DurationStyle.detectAndParse(claimDuration);

        if (this.claimDuration.isZero() || this.claimDuration.isNegative()) {
            throw new IllegalArgumentException("Claim duration must be positive");
        }
    }

    public record ClaimedNotification(Notification notification, String token) {}

    public ClaimedNotification claimNext() {
        // If another worker reserves the candidate first, read the queue again.
        for (int attempt = 0; attempt < 3; attempt++) {
            LocalDateTime now = LocalDateTime.now(clock);

            List<Long> candidates = repository.findClaimCandidates(
                    NotificationStatus.PENDING,
                    now,
                    NotificationPriority.HIGH,
                    NotificationPriority.NORMAL,
                    policy.normalCutoff(now),
                    policy.highCutoff(now),
                    PageRequest.of(0, 1));

            if (candidates.isEmpty()) {
                return null;
            }

            Long id = candidates.getFirst();
            String token = UUID.randomUUID().toString();

            ClaimedNotification claim = transactionTemplate.execute(transaction -> {
                Notification notification = repository.findLockedById(id).orElse(null);
                LocalDateTime claimedAt = LocalDateTime.now(clock);

                if (notification == null || !notification.tryClaim(
                        token, claimedAt, claimedAt.plus(claimDuration))) {
                    return null;
                }

                return new ClaimedNotification(notification, token);
            });

            if (claim != null) {
                return claim;
            }
        }

        return null;
    }
}