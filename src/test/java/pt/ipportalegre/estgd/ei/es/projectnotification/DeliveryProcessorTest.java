package pt.ipportalegre.estgd.ei.es.projectnotification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import pt.ipportalegre.estgd.ei.es.projectnotification.delivery.DeliveryProcessor;
import pt.ipportalegre.estgd.ei.es.projectnotification.delivery.RetryPolicy;
import pt.ipportalegre.estgd.ei.es.projectnotification.entity.DeliveryAttempt;
import pt.ipportalegre.estgd.ei.es.projectnotification.entity.Notification;
import pt.ipportalegre.estgd.ei.es.projectnotification.entity.NotificationChannel;
import pt.ipportalegre.estgd.ei.es.projectnotification.entity.NotificationStatus;
import pt.ipportalegre.estgd.ei.es.projectnotification.repository.NotificationRepository;
import pt.ipportalegre.estgd.ei.es.projectnotification.sender.FailureSimulatingSender;
import pt.ipportalegre.estgd.ei.es.projectnotification.sender.NotificationSender;
import pt.ipportalegre.estgd.ei.es.projectnotification.sender.SimulatedDeliveryException;
import pt.ipportalegre.estgd.ei.es.projectnotification.sender.PermanentDeliveryException;
import pt.ipportalegre.estgd.ei.es.projectnotification.sender.RetryableDeliveryException;
import pt.ipportalegre.estgd.ei.es.projectnotification.entity.NotificationPriority;
import pt.ipportalegre.estgd.ei.es.projectnotification.sender.NotificationSenderRegistry;

import java.util.function.Consumer;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeliveryProcessorTest {

    private static final int MAX_ATTEMPTS = 3;

    /** Default retry delay. Must be > 0, otherwise a failed notification is due again inside the same poll. */
    private static final Duration DEFAULT_RETRY_DELAY = Duration.ofMinutes(1);

    /** Time that passes between two scheduler ticks; longer than any retry delay used in these tests. */
    private static final Duration TICK_INTERVAL = Duration.ofMinutes(10);

    private static final Long NOTIFICATION_ID = 1L;

    @Mock
    private NotificationRepository repository;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private Clock clock;

    private Notification notification;

    private Instant currentTime;

    @BeforeEach
    void setUp() {
        currentTime = Instant.parse("2026-11-25T15:00:00Z");
        when(clock.instant()).thenAnswer(invocation -> currentTime);
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);

        // The channel must match the channel of the test senders (EMAIL),
        // otherwise the registry has no sender and the delivery fails permanently.
        notification = new Notification(List.of("a@example.com"), "Subject", "Body", NotificationChannel.EMAIL);
        ReflectionTestUtils.setField(notification, "id", NOTIFICATION_ID);

        // The repository behaves like a tiny in-memory store holding this one notification:
        // it only returns it as a claim candidate while it is PENDING and due.
        when(repository.findClaimCandidates(
                eq(NotificationStatus.PENDING),
                any(LocalDateTime.class),
                eq(NotificationPriority.HIGH),
                eq(NotificationPriority.NORMAL),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                any(Pageable.class)))
                .thenAnswer(invocation -> {
                    LocalDateTime now = invocation.getArgument(1);
                    LocalDateTime nextAttemptAt = notification.getNextAttemptAt();

                    boolean due = notification.getStatus() == NotificationStatus.PENDING
                            && (nextAttemptAt == null || !nextAttemptAt.isAfter(now));

                    return due ? List.of(NOTIFICATION_ID) : List.<Long>of();
                });

        when(repository.findLockedById(NOTIFICATION_ID))
                .thenReturn(Optional.of(notification));
    }

    private void advanceTime(Duration duration) {
        currentTime = currentTime.plus(duration);
    }

    /** Builds a sender for the EMAIL channel whose send() runs the given behavior. */
    private NotificationSender senderOf(Consumer<Notification> behavior) {
        return new NotificationSender() {
            @Override
            public NotificationChannel channel() {
                return NotificationChannel.EMAIL;
            }

            @Override
            public void send(Notification notification) {
                behavior.accept(notification);
            }
        };
    }

    private DeliveryProcessor processorWith(NotificationSender sender) {
        return processorWith(sender, DEFAULT_RETRY_DELAY);
    }

    private DeliveryProcessor processorWith(NotificationSender sender, Duration retryDelay) {
        return new DeliveryProcessor(repository,
                new NotificationSenderRegistry(List.of(sender)),
                new TransactionTemplate(transactionManager),
                clock,
                new RetryPolicy(MAX_ATTEMPTS, retryDelay));
    }

    /** Simulated sender that fails the first {@code failures} attempts of each notification. */
    private NotificationSender failingFirst(int failures) {
        return new FailureSimulatingSender(NotificationChannel.EMAIL, failures);
    }

    /** One scheduler tick: process what is due, then let time pass so any retry becomes due. */
    private void tick(DeliveryProcessor processor) {
        processor.processDueNotifications();
        advanceTime(TICK_INTERVAL);
    }

    /** Runs scheduler ticks until nothing is pending anymore (bounded, to avoid endless loops). */
    private void runUntilSettled(DeliveryProcessor processor) {
        for (int i = 0; i < MAX_ATTEMPTS + 2; i++) {
            tick(processor);
        }
    }

    // Scenario 1: successful delivery

    @Test
    void success_firstAttemptMarksNotificationSent() {
        DeliveryProcessor processor = processorWith(failingFirst(0));

        runUntilSettled(processor);

        assertEquals(NotificationStatus.SENT, notification.getStatus());
        assertEquals(1, notification.getAttemptCount());
        assertEquals(1, notification.getAttempts().size());
        assertTrue(notification.getAttempts().getFirst().isSuccess());
        assertNull(notification.getAttempts().getFirst().getErrorMessage());
    }

    // Scenario 2: failure followed by a successful attempt

    @Test
    void failureThenSuccess_isRetriedAndEndsSent() {
        Duration retryDelay = Duration.ofMinutes(1);
        DeliveryProcessor processor = processorWith(failingFirst(1), retryDelay);

        processor.processDueNotifications();

        // After the first failure the notification must still be alive and retryable
        assertEquals(NotificationStatus.PENDING, notification.getStatus());
        assertEquals(1, notification.getAttemptCount());

        advanceTime(retryDelay);
        processor.processDueNotifications();

        assertEquals(NotificationStatus.SENT, notification.getStatus());
        assertEquals(2, notification.getAttemptCount());

        List<DeliveryAttempt> history = notification.getAttempts();
        assertFalse(history.get(0).isSuccess());
        assertNotNull(history.get(0).getErrorMessage());
        assertTrue(history.get(1).isSuccess());
    }

    // Scenario 3: several consecutive failures

    @Test
    void severalFailuresBelowLimit_staysPendingUntilItSucceeds() {
        DeliveryProcessor processor = processorWith(failingFirst(MAX_ATTEMPTS - 1));

        tick(processor);
        tick(processor);

        // Two failures in a row: not given up yet
        assertEquals(NotificationStatus.PENDING, notification.getStatus());
        assertEquals(2, notification.getAttemptCount());

        runUntilSettled(processor);

        assertEquals(NotificationStatus.SENT, notification.getStatus());
        assertEquals(MAX_ATTEMPTS, notification.getAttemptCount());
    }

    // Scenario 4: maximum number of attempts reached

    @Test
    void maxAttemptsReached_marksFailedAndStopsRetrying() {
        DeliveryProcessor processor = processorWith(failingFirst(-1)); // always fails

        runUntilSettled(processor);

        assertEquals(NotificationStatus.FAILED, notification.getStatus());
        assertEquals(MAX_ATTEMPTS, notification.getAttemptCount());
        assertEquals(MAX_ATTEMPTS, notification.getAttempts().size());
        assertTrue(notification.getAttempts().stream().noneMatch(DeliveryAttempt::isSuccess));

        // Terminal state: further ticks must not try again
        processor.processDueNotifications();
        assertEquals(MAX_ATTEMPTS, notification.getAttemptCount());
    }

    // Extra guarantees

    @Test
    void failedNotificationIsNeverDeleted() {
        DeliveryProcessor processor = processorWith(failingFirst(-1));

        runUntilSettled(processor);

        verify(repository, never()).delete(any());
        verify(repository, never()).deleteById(any());
        assertNotNull(notification.getSubject());
    }

    @Test
    void exceptionTypeOfFailureIsStoredInHistory() {
        NotificationSender failing = senderOf(n -> { throw new IllegalStateException("SMTP down"); });
        DeliveryProcessor processor = processorWith(failing);

        processor.processDueNotifications();

        assertEquals("IllegalStateException: SMTP down",
                notification.getAttempts().getFirst().getErrorMessage());
    }

    @Test
    void retryIsDelayedUntilNextAttemptAt() {
        Duration retryDelay = Duration.ofMinutes(10);
        DeliveryProcessor processor = processorWith(failingFirst(-1), retryDelay);
        LocalDateTime startedAt = LocalDateTime.now(clock);

        processor.processDueNotifications();
        assertEquals(NotificationStatus.PENDING, notification.getStatus());
        assertEquals(startedAt.plus(retryDelay), notification.getNextAttemptAt());

        // One second before the retry is due: nothing happens
        advanceTime(retryDelay.minusSeconds(1));
        processor.processDueNotifications();
        assertEquals(1, notification.getAttemptCount());

        // Retry becomes due
        advanceTime(Duration.ofSeconds(1));
        processor.processDueNotifications();
        assertEquals(2, notification.getAttemptCount());
    }

    @Test
    void simulatedSenderFailsOnlyUntilConfiguredNumberOfAttempts() {
        FailureSimulatingSender sender = new FailureSimulatingSender(NotificationChannel.EMAIL, 1);

        assertThrows(
                SimulatedDeliveryException.class,
                () -> sender.send(notification)
        );

        notification.recordAttempt(false, "Simulated failure", LocalDateTime.now());

        assertDoesNotThrow(() -> sender.send(notification));
    }

    @Test
    void permanentFailure_marksFailedImmediatelyAndStopsRetrying() {
        NotificationSender failing = senderOf(n -> {
            throw new PermanentDeliveryException("Invalid recipient");
        });

        DeliveryProcessor processor = processorWith(failing);

        processor.processDueNotifications();

        assertEquals(NotificationStatus.FAILED, notification.getStatus());
        assertEquals(1, notification.getAttemptCount());
        assertEquals(1, notification.getAttempts().size());
        assertFalse(notification.getAttempts().getFirst().isSuccess());
        assertEquals(
                "PermanentDeliveryException: Invalid recipient",
                notification.getAttempts().getFirst().getErrorMessage()
        );
        assertNull(notification.getNextAttemptAt());

        // A terminal notification must not be attempted again.
        processor.processDueNotifications();

        assertEquals(1, notification.getAttemptCount());
    }

    @Test
    void unexpectedFailure_marksFailedImmediatelyAndStopsRetrying() {
        NotificationSender failing = senderOf(n -> {
            throw new IllegalStateException("Unexpected programming error");
        });

        DeliveryProcessor processor = processorWith(failing);

        processor.processDueNotifications();

        assertEquals(NotificationStatus.FAILED, notification.getStatus());
        assertEquals(1, notification.getAttemptCount());
        assertEquals(1, notification.getAttempts().size());
        assertFalse(notification.getAttempts().getFirst().isSuccess());
        assertEquals(
                "IllegalStateException: Unexpected programming error",
                notification.getAttempts().getFirst().getErrorMessage()
        );
        assertNull(notification.getNextAttemptAt());

        processor.processDueNotifications();

        assertEquals(1, notification.getAttemptCount());
    }

    @Test
    void retryableFailure_staysPendingUntilMaximumAttempts() {
        NotificationSender failing = senderOf(n -> {
            throw new RetryableDeliveryException("Temporary SMTP failure");
        });

        DeliveryProcessor processor = processorWith(failing);

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            tick(processor);

            if (attempt < MAX_ATTEMPTS) {
                assertEquals(NotificationStatus.PENDING, notification.getStatus());
            }
        }

        assertEquals(NotificationStatus.FAILED, notification.getStatus());
        assertEquals(MAX_ATTEMPTS, notification.getAttemptCount());
        assertEquals(MAX_ATTEMPTS, notification.getAttempts().size());
        assertNull(notification.getNextAttemptAt());
    }
}