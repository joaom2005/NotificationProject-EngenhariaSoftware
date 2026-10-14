package pt.ipportalegre.estgd.ei.es.projectnotification;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import pt.ipportalegre.estgd.ei.es.projectnotification.dto.NotificationRequest;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private NotificationRequest validRequest() {
        NotificationRequest request = new NotificationRequest();
        request.setSubject("Subject");
        request.setMessage("Message");
        request.setRecipients(List.of("a@example.com", "b@example.com"));
        return request;
    }

    @Test
    void validRequest_hasNoViolations() {
        assertTrue(validator.validate(validRequest()).isEmpty());
    }

    @Test
    void blankSubject_isRejected() {
        NotificationRequest request = validRequest();
        request.setSubject("  ");

        Set<ConstraintViolation<NotificationRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        assertEquals("subject", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    void subjectLongerThan255_isRejected() {
        NotificationRequest request = validRequest();
        request.setSubject("x".repeat(256));

        Set<ConstraintViolation<NotificationRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        assertEquals("subject", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    void blankMessage_isRejected() {
        NotificationRequest request = validRequest();
        request.setMessage("");

        Set<ConstraintViolation<NotificationRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        assertEquals("message", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    void emptyRecipients_isRejected() {
        NotificationRequest request = validRequest();
        request.setRecipients(List.of());

        Set<ConstraintViolation<NotificationRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        assertEquals("Enter at least one recipient", violations.iterator().next().getMessage());
    }

    @Test
    void invalidEmailAddress_isRejected() {
        NotificationRequest request = validRequest();
        request.setRecipients(List.of("a@example.com", "not-an-email"));

        Set<ConstraintViolation<NotificationRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        assertEquals("Invalid email address", violations.iterator().next().getMessage());
    }
}
