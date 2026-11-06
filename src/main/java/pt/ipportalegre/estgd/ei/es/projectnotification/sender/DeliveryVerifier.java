package pt.ipportalegre.estgd.ei.es.projectnotification.sender;

import pt.ipportalegre.estgd.ei.es.projectnotification.entity.Notification;

/**
 * Checks whether a notification was already delivered by an earlier attempt,
 * for example one whose outcome could not be recorded in the database.
 * Used by senders to avoid duplicates when retrying.
 */
public interface DeliveryVerifier {

    boolean wasAlreadyDelivered(Notification notification);
}