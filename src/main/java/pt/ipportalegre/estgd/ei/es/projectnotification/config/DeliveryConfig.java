package pt.ipportalegre.estgd.ei.es.projectnotification.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import pt.ipportalegre.estgd.ei.es.projectnotification.sender.FailureSimulatingSender;
import pt.ipportalegre.estgd.ei.es.projectnotification.sender.NotificationSender;
import pt.ipportalegre.estgd.ei.es.projectnotification.sender.NotificationSenderRegistry;

import java.time.Clock;
import java.util.List;

@Configuration
@EnableScheduling
public class DeliveryConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    /**
     * Builds the registry from the real senders, or from simulated ones when
     * app.delivery.simulate.enabled=true. The registry itself knows nothing about simulation.
     */
    @Bean
    public NotificationSenderRegistry notificationSenderRegistry(
            List<NotificationSender> realSenders,
            @Value("${app.delivery.simulate.enabled:false}") boolean simulate,
            @Value("${app.delivery.simulate.failures-per-notification:2}") int failures) {

        if (!simulate) {
            return new NotificationSenderRegistry(realSenders);
        }

        List<NotificationSender> simulated = realSenders.stream()
                .<NotificationSender>map(s -> new FailureSimulatingSender(s.channel(), failures))
                .toList();
        return new NotificationSenderRegistry(simulated);
    }
}