
package pt.ipportalegre.estgd.ei.es.projectnotification.sender;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import pt.ipportalegre.estgd.ei.es.projectnotification.entity.Notification;

/**
 * Simulates notification delivery for development and demonstrations.
 * Fails the configured number of attempts, then reports success without
 * sending a real email.
 * Only active when app.delivery.simulate.enabled=true.
 */
@Component
@Primary
@ConditionalOnProperty(
        name = "app.delivery.simulate.enabled",
        havingValue = "true"
)
public class FailureSimulatingSender implements NotificationSender {

    private final int failuresPerNotification;

    public FailureSimulatingSender(
            @Value("${app.delivery.simulate.failures-per-notification:2}")
            int failuresPerNotification) {
        this.failuresPerNotification = failuresPerNotification;
    }

    @Override
    public void send(Notification notification) {
        boolean alwaysFail = failuresPerNotification < 0;

        if (alwaysFail ||
                notification.getAttemptCount() < failuresPerNotification) {
            throw new SimulatedDeliveryException(
                    "Simulated failure on attempt "
                            + (notification.getAttemptCount() + 1)
            );
        }

        // Simulated success: deliberately do not send a real email.
    }
}
