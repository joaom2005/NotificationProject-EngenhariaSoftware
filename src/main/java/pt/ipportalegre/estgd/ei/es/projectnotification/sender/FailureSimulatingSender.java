package pt.ipportalegre.estgd.ei.es.projectnotification.sender;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import pt.ipportalegre.estgd.ei.es.projectnotification.entity.Notification;

/**
 * Decorator that makes the real email sender fail on purpose, for development and demos.
 *
 * <p>Only active with {@code app.delivery.simulate.enabled=true}. Being {@link Primary}, it
 * replaces the plain sender everywhere a {@link NotificationSender} is injected, so no other
 * class needs to know about the simulation.
 *
 * <p>Failures are decided per notification, not globally: a notification fails while it has
 * fewer previous attempts than the configured number, then the call goes to the real sender.
 * No state is kept here, so it is also correct after an application restart.
 */
@Component
@Primary
@ConditionalOnProperty(name = "app.delivery.simulate.enabled", havingValue = "true")
public class FailureSimulatingSender implements NotificationSender {

    private final NotificationSender delegate;
    private final int failuresPerNotification;

    public FailureSimulatingSender(
            EmailSender delegate,
            @Value("${app.delivery.simulate.failures-per-notification:2}") int failuresPerNotification) {
        this.delegate = delegate;
        this.failuresPerNotification = failuresPerNotification;
    }

    @Override
    public void send(Notification notification) {
        // attemptCount is the number of attempts made BEFORE this one
        boolean alwaysFail = failuresPerNotification < 0;
        if (alwaysFail || notification.getAttemptCount() < failuresPerNotification) {
            throw new SimulatedDeliveryException(
                    "Simulated failure on attempt " + (notification.getAttemptCount() + 1));
        }
        delegate.send(notification);
    }
}
