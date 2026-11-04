package pt.ipportalegre.estgd.ei.es.projectnotification.entity;

/**
 * Lifecycle of a notification with respect to its delivery.
 *
 * <pre>
 * PENDING --(send ok)--------------------------> SENT
 * PENDING --(send fails, attempts left)--------> PENDING (retried later)
 * PENDING --(send fails, no attempts left)-----> FAILED
 * </pre>
 */
public enum NotificationStatus {

    /** Saved, but not delivered yet. Eligible for (re)processing. */
    PENDING,

    /** Delivered successfully. Terminal state. */
    SENT,

    /** Delivery was abandoned after reaching the maximum number of attempts. Terminal state. */
    FAILED
}
