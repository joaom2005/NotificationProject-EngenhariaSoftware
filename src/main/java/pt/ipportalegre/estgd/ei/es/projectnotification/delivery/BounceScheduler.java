package pt.ipportalegre.estgd.ei.es.projectnotification.delivery;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.delivery.bounce.enabled", havingValue = "true")
public class BounceScheduler {

    private final BounceProcessor bounceProcessor;

    public BounceScheduler(BounceProcessor bounceProcessor) {
        this.bounceProcessor = bounceProcessor;
    }

    @Scheduled(fixedDelayString = "${app.delivery.bounce.poll-delay-ms:60000}")
    public void checkBounces() {
        bounceProcessor.processRecentBounces();
    }
}