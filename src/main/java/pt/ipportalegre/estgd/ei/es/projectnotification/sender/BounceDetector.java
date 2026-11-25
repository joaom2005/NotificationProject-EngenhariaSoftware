package pt.ipportalegre.estgd.ei.es.projectnotification.sender;

import pt.ipportalegre.estgd.ei.es.projectnotification.entity.Notification;

import java.util.Optional;

/**
 * Checks whether a notification that the mail server accepted was rejected afterwards
 * (a bounce). Separate from DeliveryVerifier on purpose: that one answers "was it already
 * sent?" before sending, this one answers "was it rejected?" after sending.
 */
public interface BounceDetector {

    /** @return the bounce reason (e.g. the failed recipients), or empty if no bounce was found */
    Optional<String> findBounce(Notification notification);
}