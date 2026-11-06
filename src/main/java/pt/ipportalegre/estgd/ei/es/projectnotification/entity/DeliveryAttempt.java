package pt.ipportalegre.estgd.ei.es.projectnotification.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * One try at delivering a notification. Rows are only ever inserted, never updated,
 * so the table doubles as the processing history of a notification.
 */
@Entity
@Table(name = "delivery_attempts")
public class DeliveryAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    /** 1-based position of this attempt within its notification. */
    @Column(nullable = false)
    private int attemptNumber;

    @Column(nullable = false)
    private LocalDateTime attemptedAt;

    @Column(nullable = false)
    private boolean success;

    /** Failure reason. Null when the attempt succeeded. */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    protected DeliveryAttempt() {
        // Required by JPA
    }

    DeliveryAttempt(Notification notification, int attemptNumber, boolean success,
                    String errorMessage, LocalDateTime attemptedAt) {
        this.notification = notification;
        this.attemptNumber = attemptNumber;
        this.success = success;
        this.errorMessage = errorMessage;
        this.attemptedAt = attemptedAt;
    }

    public Long getId() {
        return id;
    }

    public Notification getNotification() {
        return notification;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public LocalDateTime getAttemptedAt() {
        return attemptedAt;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
