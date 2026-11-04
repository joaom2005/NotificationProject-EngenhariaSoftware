package pt.ipportalegre.estgd.ei.es.projectnotification.delivery;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Trigger of the delivery process: wakes up periodically and asks the processor
 * to handle whatever is due. Contains no delivery logic on purpose, so the
 * trigger can be replaced (event, queue, ...) without touching the processor.
 *
 * <p>fixedDelay (not fixedRate) guarantees a run never overlaps with the previous
 * one, so the same notification is never processed twice at the same time.
 */
@Component
public class DeliveryScheduler {

    private final DeliveryProcessor processor;

    public DeliveryScheduler(DeliveryProcessor processor) {
        this.processor = processor;
    }

    @Scheduled(fixedDelayString = "${app.delivery.poll-interval-ms:5000}")
    public void poll() {
        processor.processDueNotifications();
    }
}
