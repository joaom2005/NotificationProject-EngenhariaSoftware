package pt.ipportalegre.estgd.ei.es.projectnotification.delivery;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.FixedDelayTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Trigger of the delivery process: wakes up periodically and asks the processor
 * to handle whatever is due. Contains no delivery logic on purpose, so the
 * trigger can be replaced (event, queue, ...) without touching the processor.
 *
 * <p>fixedDelay (not fixedRate) guarantees a run never overlaps with the previous
 * one, so the same notification is never processed twice at the same time.
 */
@Component
public class DeliveryScheduler implements SchedulingConfigurer {

    private final DeliveryProcessor processor;
    private final int workers;
    private final Duration pollInterval;

    public DeliveryScheduler(
            DeliveryProcessor processor,
            @Value("${app.delivery.workers:2}") int workers,
            @Value("${app.delivery.poll-interval-ms:5000}") long pollIntervalMs) {
        if (workers < 1 || pollIntervalMs < 1) {
            throw new IllegalArgumentException(
                    "Workers and poll interval must be positive");
        }

        this.processor = processor;
        this.workers = workers;
        this.pollInterval = Duration.ofMillis(pollIntervalMs);
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        for (int worker = 0; worker < workers; worker++) {
            registrar.addFixedDelayTask(new FixedDelayTask(
                    processor::processDueNotifications, pollInterval, Duration.ZERO));
        }
    }
}