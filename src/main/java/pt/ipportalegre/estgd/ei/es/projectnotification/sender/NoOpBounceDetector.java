package pt.ipportalegre.estgd.ei.es.projectnotification.sender;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import pt.ipportalegre.estgd.ei.es.projectnotification.entity.Notification;

import java.util.Optional;

/** Default detector: never finds a bounce, so notifications simply stay SENT. */
@Component
@ConditionalOnProperty(name = "app.delivery.bounce.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpBounceDetector implements BounceDetector {

    @Override
    public Optional<String> findBounce(Notification notification) {
        return Optional.empty();
    }
}