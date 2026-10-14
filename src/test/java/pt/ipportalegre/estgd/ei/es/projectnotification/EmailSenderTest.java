package pt.ipportalegre.estgd.ei.es.projectnotification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import pt.ipportalegre.estgd.ei.es.projectnotification.sender.EmailSender;
import pt.ipportalegre.estgd.ei.es.projectnotification.entity.Notification;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailSenderTest {

    @Mock
    private JavaMailSender mailSender;

    @Test
    void send_buildsSingleMessageWithAllRecipients() {
        EmailSender sender = new EmailSender(mailSender, "noreply@example.com");
        Notification notification = new Notification(
                List.of("a@example.com", "b@example.com"), "Subject", "Body");

        sender.send(notification);

        // One notification must produce exactly one email
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertArrayEquals(new String[]{"a@example.com", "b@example.com"}, sent.getTo());
        assertEquals("Subject", sent.getSubject());
        assertEquals("Body", sent.getText());
        assertEquals("noreply@example.com", sent.getFrom());
    }

    @Test
    void send_withBlankFromAddress_doesNotSetFrom() {
        EmailSender sender = new EmailSender(mailSender, "");
        Notification notification = new Notification(List.of("a@example.com"), "Subject", "Body");

        sender.send(notification);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertNull(captor.getValue().getFrom());
    }

    @Test
    void send_withoutRecipients_throwsAndSendsNothing() {
        EmailSender sender = new EmailSender(mailSender, "noreply@example.com");
        Notification notification = new Notification(List.of(), "Subject", "Body");

        assertThrows(IllegalArgumentException.class, () -> sender.send(notification));

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }
}