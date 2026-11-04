package pt.ipportalegre.estgd.ei.es.projectnotification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@Configuration
@EnableScheduling
public class DeliveryConfig {

    /** Injected wherever "now" is needed, so tests can replace it with a fixed clock. */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
