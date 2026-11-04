package pt.ipportalegre.estgd.ei.es.projectnotification.sender;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import pt.ipportalegre.estgd.ei.es.projectnotification.entity.Notification;

import java.util.List;

@Component
public class EmailSender implements NotificationSender {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailSender(
            JavaMailSender mailSender,
            @Value("${app.mail.from:}") String fromAddress) {
        this.mailSender = mailSender;
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

        SimpleMailMessage message = new SimpleMailMessage();

        if (StringUtils.hasText(fromAddress)) {
            message.setFrom(fromAddress);
        }

        message.setTo(recipients.toArray(String[]::new));
        message.setSubject(notification.getSubject());
        message.setText(notification.getMessage());

        try {
            mailSender.send(message);
        } catch (MailException e) {
            throw new RetryableDeliveryException(
                    "Email delivery failed: " + e.getMessage(), e);
        }
    }
}