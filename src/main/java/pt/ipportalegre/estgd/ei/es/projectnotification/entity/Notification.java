package pt.ipportalegre.estgd.ei.es.projectnotification.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "notification_recipient_addresses",
            joinColumns = @JoinColumn(name = "notification_id")
    )
    @Column(name = "address", nullable = false)
    private List<String> recipients = new ArrayList<>();

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // Delivery tracking

    // The column default only matters for rows created before delivery tracking existed:
    // they were sent synchronously, so SENT is correct and the scheduler will not resend them.
    // New notifications always get an explicit PENDING from this field initializer.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20) default 'SENT'")
    private NotificationStatus status = NotificationStatus.PENDING;

    /** Number of delivery attempts made so far (successful or not). */
    @Column(nullable = false)
    private int attemptCount = 0;

    /** Earliest moment the next attempt may run. Null means "as soon as possible". */
    private LocalDateTime nextAttemptAt;

    // LAZY on purpose: 'recipients' is already an EAGER List, and fetching two
    // eager Lists at once makes Hibernate throw MultipleBagFetchException.
    @OneToMany(mappedBy = "notification", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("attemptNumber ASC")
    private List<DeliveryAttempt> attempts = new ArrayList<>();

    // Constructors

    public Notification() {
        this.createdAt = LocalDateTime.now();
    }

    public Notification(List<String> recipients, String subject, String message) {
        this.recipients = new ArrayList<>(recipients);
        this.subject = subject;
        this.message = message;
        this.createdAt = LocalDateTime.now();
    }

    // Delivery behaviour

    /**
     * Records one delivery attempt: appends it to the history and bumps the counter.
     * Does NOT change the status; deciding between PENDING, SENT and FAILED
     * is the job of the delivery component, which knows the retry policy.
     */
    public DeliveryAttempt recordAttempt(boolean success, String errorMessage, LocalDateTime attemptedAt) {
        attemptCount++;
        DeliveryAttempt attempt = new DeliveryAttempt(this, attemptCount, success, errorMessage, attemptedAt);
        attempts.add(attempt);
        return attempt;
    }

    public void markSent() {
        requirePending();
        this.status = NotificationStatus.SENT;
        this.nextAttemptAt = null;
    }

    /** PENDING -> FAILED. Terminal: no more attempts will be made. */
    public void markFailed() {
        requirePending();
        this.status = NotificationStatus.FAILED;
        this.nextAttemptAt = null;
    }

    /** PENDING -> PENDING, but not eligible for processing until the given moment. */
    public void scheduleRetry(LocalDateTime nextAttemptAt) {
        requirePending();
        this.nextAttemptAt = Objects.requireNonNull(nextAttemptAt, "nextAttemptAt must not be null");
    }

    private void requirePending() {
        if (status != NotificationStatus.PENDING) {
            throw new IllegalStateException(
                    "Notification " + id + " is " + status + " and can no longer change state");
        }
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public List<String> getRecipients() { return recipients; }
    public void setRecipients(List<String> recipients) { this.recipients = recipients; }

    public String getSubject() {
        return subject;
    }
    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public LocalDateTime getNextAttemptAt() {
        return nextAttemptAt;
    }

    /** Read-only view of the delivery history, oldest attempt first. */
    public List<DeliveryAttempt> getAttempts() {
        return Collections.unmodifiableList(attempts);
    }
}
