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

import pt.ipportalegre.estgd.ei.es.projectnotification.delivery.DeliveryProcessor;
import pt.ipportalegre.estgd.ei.es.projectnotification.delivery.RetryPolicy;
import pt.ipportalegre.estgd.ei.es.projectnotification.entity.DeliveryAttempt;
import pt.ipportalegre.estgd.ei.es.projectnotification.entity.Notification;
import pt.ipportalegre.estgd.ei.es.projectnotification.entity.NotificationStatus;
import pt.ipportalegre.estgd.ei.es.projectnotification.repository.NotificationRepository;
import pt.ipportalegre.estgd.ei.es.projectnotification.sender.FailureSimulatingSender;
import pt.ipportalegre.estgd.ei.es.projectnotification.sender.NotificationSender;
import pt.ipportalegre.estgd.ei.es.projectnotification.sender.SimulatedDeliveryException;
import pt.ipportalegre.estgd.ei.es.projectnotification.sender.PermanentDeliveryException;
import pt.ipportalegre.estgd.ei.es.projectnotification.sender.RetryableDeliveryException;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeliveryProcessorTest {

    private static final int MAX_ATTEMPTS = 3;

    @Mock
    private NotificationRepository repository;

    @Mock
    private PlatformTransactionManager transactionManager;

    private Notification notification;

    @BeforeEach
    void setUp() {
        notification = new Notification(List.of("a@example.com"), "Subject", "Body");

        // The repository behaves like a tiny in-memory store holding this one notification:
        // it only returns it from findDue while it is still PENDING.
        when(repository.findDue(eq(NotificationStatus.PENDING), any(LocalDateTime.class)))
                .thenAnswer(inv -> notification.getStatus() == NotificationStatus.PENDING
                        ? List.of(notification) : List.of());
        when(repository.findById(any())).thenAnswer(inv -> Optional.of(notification));
    }

    private DeliveryProcessor processorWith(NotificationSender sender) {
        return new DeliveryProcessor(repository, sender,
                new TransactionTemplate(transactionManager),
                Clock.systemDefaultZone(),
                new RetryPolicy(MAX_ATTEMPTS, Duration.ZERO));
    }

    /** Simulated sender that fails the first {@code failures} attempts of each notification. */
    private NotificationSender failingFirst(int failures) {
        return new FailureSimulatingSender(failures);
    }

    /** Runs scheduler ticks until nothing is pending anymore (bounded, to avoid endless loops). */
    private void runUntilSettled(DeliveryProcessor processor) {
        for (int tick = 0; tick < MAX_ATTEMPTS + 2; tick++) {
            processor.processDueNotifications();
        }
    }

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

    @Test
    void failureThenSuccess_isRetriedAndEndsSent() {
        DeliveryProcessor processor = processorWith(failingFirst(1));

        processor.processDueNotifications();
        // After the first failure the notification must still be alive and retryable
        assertEquals(NotificationStatus.PENDING, notification.getStatus());
        assertEquals(1, notification.getAttemptCount());

        runUntilSettled(processor);

        assertEquals(NotificationStatus.SENT, notification.getStatus());
        assertEquals(2, notification.getAttemptCount());
        List<DeliveryAttempt> history = notification.getAttempts();
        assertFalse(history.get(0).isSuccess());
        assertNotNull(history.get(0).getErrorMessage());
        assertTrue(history.get(1).isSuccess());
    }

    @Test
    void severalFailuresBelowLimit_staysPendingUntilItSucceeds() {
        DeliveryProcessor processor = processorWith(failingFirst(MAX_ATTEMPTS - 1));

        processor.processDueNotifications();
        processor.processDueNotifications();

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
        NotificationSender failing = n -> { throw new IllegalStateException("SMTP down"); };
        DeliveryProcessor processor = processorWith(failing);

        processor.processDueNotifications();

        assertEquals("IllegalStateException: SMTP down",
                notification.getAttempts().getFirst().getErrorMessage());
    }

    @Test
    void retryIsDelayedUntilNextAttemptAt() {
        DeliveryProcessor delayed = new DeliveryProcessor(repository, failingFirst(-1),
                new TransactionTemplate(transactionManager),
                Clock.systemDefaultZone(),
                new RetryPolicy(MAX_ATTEMPTS, Duration.ofMinutes(10)));

        LocalDateTime before = LocalDateTime.now();
        delayed.processDueNotifications();

        assertEquals(NotificationStatus.PENDING, notification.getStatus());
        assertNotNull(notification.getNextAttemptAt());
        assertTrue(notification.getNextAttemptAt().isAfter(before.plusMinutes(9)));
    }

    @Test
    void simulatedSenderFailsOnlyUntilConfiguredNumberOfAttempts() {
        FailureSimulatingSender sender = new FailureSimulatingSender(1);

        assertThrows(
                SimulatedDeliveryException.class,
                () -> sender.send(notification)
        );

        notification.recordAttempt(false, "Simulated failure", LocalDateTime.now());

        assertDoesNotThrow(() -> sender.send(notification));
    }
    @Test
    void permanentFailure_marksFailedImmediatelyAndStopsRetrying() {
        NotificationSender failing = n -> {
            throw new PermanentDeliveryException("Invalid recipient");
        };

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
        NotificationSender failing = n -> {
            throw new IllegalStateException("Unexpected programming error");
        };

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
        NotificationSender failing = n -> {
            throw new RetryableDeliveryException("Temporary SMTP failure");
        };

        DeliveryProcessor processor = processorWith(failing);

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            processor.processDueNotifications();

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
