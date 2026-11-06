package pt.ipportalegre.estgd.ei.es.projectnotification.delivery;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Decides whether a failed notification may be retried and when.
 * Pure logic with no I/O, so it can be tested without mocks and swapped
 * (e.g. for exponential backoff) without touching the processor.
 */
@Component
public class RetryPolicy {

    private final int maxAttempts;
    private final Duration retryDelay;

    public RetryPolicy(
            @Value("${app.delivery.max-attempts:5}") int maxAttempts,
            @Value("${app.delivery.retry-delay:30s}") Duration retryDelay) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("Maximum attempts must be at least 1");
        }

        if (retryDelay.isNegative()) {
            throw new IllegalArgumentException("Retry delay cannot be negative");
        }

        this.maxAttempts = maxAttempts;
        this.retryDelay = retryDelay;
    }

    /** True if another attempt is still allowed after {@code attemptsMade} attempts. */
    public boolean hasAttemptsLeft(int attemptsMade) {
        return attemptsMade < maxAttempts;
    }

    /** Linear backoff: after the n-th failure, wait n * retryDelay. */
    public LocalDateTime nextAttemptAt(int attemptsMade, LocalDateTime now) {
        return now.plus(retryDelay.multipliedBy(attemptsMade));
    }
}