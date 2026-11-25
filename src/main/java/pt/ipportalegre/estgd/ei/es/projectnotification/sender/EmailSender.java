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
import pt.ipportalegre.estgd.ei.es.projectnotification.entity.NotificationChannel;

import java.time.Duration;
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
    private final Duration interSendDelay;

    public EmailSender(
            JavaMailSender mailSender,
            DeliveryVerifier deliveryVerifier,
            @Value("${app.mail.from:}") String fromAddress,
            @Value("${app.delivery.email.send-delay:200ms}") Duration interSendDelay) {
        this.mailSender = mailSender;
        this.deliveryVerifier = deliveryVerifier;
        this.fromAddress = fromAddress;
        this.interSendDelay = interSendDelay;
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
            throw new RetryableDeliveryException("Email delivery failed: " + e.getMessage(), e);
        }

        pauseBeforeNextSend();
    }

    /** Keeps a steady pace towards the mail provider, which limits how fast we may send. */
    private void pauseBeforeNextSend() {
        try {
            Thread.sleep(interSendDelay);
        } catch (InterruptedException e) {
            // Email already went out, so do not report a failure; just keep the interrupt flag
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
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

            message.setHeader("Message-ID", MessageIds.forNotification(notification));
            return message;
        } catch (MessagingException e) {
            throw new PermanentDeliveryException("Could not build email: " + e.getMessage());
        }
    }

}