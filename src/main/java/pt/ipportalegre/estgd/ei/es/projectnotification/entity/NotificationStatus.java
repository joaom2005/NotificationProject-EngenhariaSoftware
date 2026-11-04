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
    PENDING,
    SENT,
    FAILED
}