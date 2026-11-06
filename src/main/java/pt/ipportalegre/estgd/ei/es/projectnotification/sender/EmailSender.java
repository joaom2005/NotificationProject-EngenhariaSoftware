package pt.ipportalegre.estgd.ei.es.projectnotification.sender;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import pt.ipportalegre.estgd.ei.es.projectnotification.entity.Notification;

import java.util.List;

/**
 * Sends a notification as one email with a deterministic Message-ID.
 * Before sending, it asks the {@link DeliveryVerifier} whether an earlier attempt already
 * delivered it, which makes retries idempotent.
 */
@Component
public class EmailSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    private final JavaMailSender mailSender;
    private final DeliveryVerifier deliveryVerifier;
    private final String fromAddress;

    public EmailSender(
            JavaMailSender mailSender,
            DeliveryVerifier deliveryVerifier,
            @Value("${app.mail.from:}") String fromAddress) {
        this.mailSender = mailSender;
        this.deliveryVerifier = deliveryVerifier;
        this.fromAddress = fromAddress;
    }

    @Override
    public void send(Notification notification) {
        List<String> recipients = notification.getRecipients();

        if (recipients == null || recipients.isEmpty()
                || recipients.stream().anyMatch(r -> !StringUtils.hasText(r))) {
            throw new PermanentDeliveryException(
                    "A notification must have at least one valid recipient");
        }

        if (deliveryVerifier.wasAlreadyDelivered(notification)) {
            log.info("Notification {} was already delivered by an earlier attempt, not sending again",
                    notification.getId());
            return;
        }

        MimeMessage message = buildMessage(notification, recipients);

        try {
            mailSender.send(message);
        } catch (MailAuthenticationException e) {
            throw new PermanentDeliveryException("Mail authentication failed: " + e.getMessage());
        } catch (MailException e) {
            throw new RetryableDeliveryException(
                    "Email delivery failed: " + e.getMessage(), e);
        }
    }

    private MimeMessage buildMessage(Notification notification, List<String> recipients) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

            if (StringUtils.hasText(fromAddress)) {
                helper.setFrom(fromAddress);
            }

            helper.setTo(recipients.toArray(String[]::new));
            helper.setSubject(notification.getSubject());
            helper.setText(notification.getMessage());

            // Spring keeps a Message-ID that is already set, so retries reuse the same one
            message.setHeader("Message-ID", MessageIds.forNotification(notification));
            return message;
        } catch (MessagingException e) {
            // A message that cannot even be built will not build on a retry either
            throw new PermanentDeliveryException("Could not build email: " + e.getMessage());
        }
    }

    /** MimeMessage normally generates a new Message-ID on send; this keeps ours. */
    private static class FixedIdMimeMessage extends MimeMessage {

        private final String messageId;

        FixedIdMimeMessage(MimeMessage source, String messageId) throws MessagingException {
            super(source);
            this.messageId = messageId;
        }

        @Override
        protected void updateMessageID() throws MessagingException {
            setHeader("Message-ID", messageId);
        }
    }
}