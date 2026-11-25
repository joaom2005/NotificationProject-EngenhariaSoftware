package pt.ipportalegre.estgd.ei.es.projectnotification.entity;

/** Delivery channels a notification can be sent through. */
public enum NotificationChannel {
    EMAIL(true),
    CONSOLE(false);

    private final boolean requiresRecipients;

    NotificationChannel(boolean requiresRecipients) {
        this.requiresRecipients = requiresRecipients;
    }

    /** Whether notifications on this channel must name recipients. */
    public boolean requiresRecipients() {
        return requiresRecipients;
    }
}