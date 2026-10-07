package pt.ipportalegre.estgd.ei.es.projectnotification.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import pt.ipportalegre.estgd.ei.es.projectnotification.entity.Notification;
import pt.ipportalegre.estgd.ei.es.projectnotification.entity.Recipient;

import java.util.List;

@Component
public class EmailSender {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailSender(
            JavaMailSender mailSender,
            @Value("${app.mail.from:}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    public void send(Notification notification) {
        List<Recipient> recipients = notification.getRecipients();
        if (recipients == null || recipients.isEmpty()) {
            throw new IllegalArgumentException("A notification must have at least one recipient");
        }

        for (Recipient recipient : recipients) {
            if (!StringUtils.hasText(recipient.getAddress())) {
                throw new IllegalArgumentException("Recipient email address must not be blank");
            }

            SimpleMailMessage message = new SimpleMailMessage();
            if (StringUtils.hasText(fromAddress)) {
                message.setFrom(fromAddress);
            }
            message.setTo(recipient.getAddress());
            message.setSubject(notification.getSubject());
            message.setText(notification.getMessage());
            mailSender.send(message);
        }
    }
}