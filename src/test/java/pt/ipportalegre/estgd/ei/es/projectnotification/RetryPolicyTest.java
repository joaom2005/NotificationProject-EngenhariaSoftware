package pt.ipportalegre.estgd.ei.es.projectnotification;

import org.junit.jupiter.api.Test;

import pt.ipportalegre.estgd.ei.es.projectnotification.delivery.RetryPolicy;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class RetryPolicyTest {

    @Test
    void hasAttemptsLeft_isTrueBelowLimitAndFalseAtLimit() {
        RetryPolicy policy = new RetryPolicy(3, Duration.ofSeconds(30));

        assertTrue(policy.hasAttemptsLeft(1));
        assertTrue(policy.hasAttemptsLeft(2));
        assertFalse(policy.hasAttemptsLeft(3));
        assertFalse(policy.hasAttemptsLeft(4));
    }

    @Test
    void nextAttemptAt_growsLinearlyWithAttempts() {
        RetryPolicy policy = new RetryPolicy(5, Duration.ofSeconds(30));
        LocalDateTime now = LocalDateTime.of(2026, 11, 4, 12, 0, 0);

        assertEquals(now.plusSeconds(30), policy.nextAttemptAt(1, now));
        assertEquals(now.plusSeconds(60), policy.nextAttemptAt(2, now));
        assertEquals(now.plusSeconds(90), policy.nextAttemptAt(3, now));
    }

    @Test
    void zeroDelay_makesRetryImmediatelyDue() {
        RetryPolicy policy = new RetryPolicy(3, Duration.ZERO);
        LocalDateTime now = LocalDateTime.now();

        assertEquals(now, policy.nextAttemptAt(2, now));
    }

    @Test
    void constructor_withZeroMaxAttempts_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new RetryPolicy(0, Duration.ofSeconds(30)));
    }

    @Test
    void constructor_withNegativeDelay_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new RetryPolicy(3, Duration.ofSeconds(-1)));
    }
}