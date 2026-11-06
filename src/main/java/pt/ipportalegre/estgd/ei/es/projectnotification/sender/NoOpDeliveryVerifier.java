package pt.ipportalegre.estgd.ei.es.projectnotification.sender;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import pt.ipportalegre.estgd.ei.es.projectnotification.entity.Notification;

/** Default verifier: never finds anything, so delivery is plain at-least-once. */
@Component
@ConditionalOnProperty(name = "app.delivery.verify.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpDeliveryVerifier implements DeliveryVerifier {

    @Override
    public boolean wasAlreadyDelivered(Notification notification) {
        return false;
    }
}