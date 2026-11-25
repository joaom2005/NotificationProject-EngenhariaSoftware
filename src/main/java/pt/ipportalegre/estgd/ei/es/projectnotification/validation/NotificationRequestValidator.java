package pt.ipportalegre.estgd.ei.es.projectnotification.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import pt.ipportalegre.estgd.ei.es.projectnotification.dto.NotificationRequest;
import pt.ipportalegre.estgd.ei.es.projectnotification.entity.NotificationChannel;

import java.util.List;
import java.util.regex.Pattern;

public class NotificationRequestValidator
        implements ConstraintValidator<ValidNotificationRequest, NotificationRequest> {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    @Override
    public boolean isValid(NotificationRequest request, ConstraintValidatorContext context) {
        NotificationChannel channel = request.getChannel();

        // A missing channel is reported by @NotNull; channels without recipients need no checks
        if (channel == null || !channel.requiresRecipients()) {
            return true;
        }

        List<String> recipients = request.getRecipients();
        if (recipients == null || recipients.isEmpty()) {
            return reject(context, "Enter at least one recipient");
        }
        if (recipients.stream().anyMatch(r -> r == null || !EMAIL.matcher(r.trim()).matches())) {
            return reject(context, "Invalid email address");
        }
        return true;
    }

    private boolean reject(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode("recipients")
                .addConstraintViolation();
        return false;
    }
}