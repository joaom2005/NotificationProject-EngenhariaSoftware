package pt.ipportalegre.estgd.ei.es.projectnotification.sender;

import pt.ipportalegre.estgd.ei.es.projectnotification.entity.Notification;
import pt.ipportalegre.estgd.ei.es.projectnotification.entity.NotificationChannel;

/**
 * Stand-in sender for development and demos: fails the configured number of attempts
 * of each notification and then reports success, without delivering anything for real.
 * Not a Spring component: DeliveryConfig builds one per channel when simulation is on.
 */
public class FailureSimulatingSender implements NotificationSender {

    private final NotificationChannel channel;
    private final int failuresPerNotification;

    public FailureSimulatingSender(NotificationChannel channel, int failuresPerNotification) {
        this.channel = channel;
        this.failuresPerNotification = failuresPerNotification;
    }

    @Override
    public NotificationChannel channel() {
        return channel;
    }

    @Override
    public void send(Notification notification) {
        boolean alwaysFail = failuresPerNotification < 0;

        if (alwaysFail || notification.getAttemptCount() < failuresPerNotification) {
            throw new SimulatedDeliveryException(
                    "Simulated failure on attempt " + (notification.getAttemptCount() + 1));
        }
        // Simulated success: deliberately delivers nothing.
    }
}