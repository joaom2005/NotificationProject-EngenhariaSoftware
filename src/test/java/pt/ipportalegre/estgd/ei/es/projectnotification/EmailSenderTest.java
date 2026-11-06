package pt.ipportalegre.estgd.ei.es.projectnotification;

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import pt.ipportalegre.estgd.ei.es.projectnotification.entity.Notification;
import pt.ipportalegre.estgd.ei.es.projectnotification.sender.DeliveryVerifier;
import pt.ipportalegre.estgd.ei.es.projectnotification.sender.EmailSender;
import pt.ipportalegre.estgd.ei.es.projectnotification.sender.PermanentDeliveryException;
import pt.ipportalegre.estgd.ei.es.projectnotification.sender.RetryableDeliveryException;

import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailSenderTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private DeliveryVerifier verifier;

    @BeforeEach
    void setUp() {
        lenient().when(mailSender.createMimeMessage())
                .thenAnswer(inv -> new MimeMessage(Session.getInstance(new Properties())));
    }

    private MimeMessage sendAndCapture(EmailSender sender, Notification notification) {
        sender.send(notification);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }

    @Test
    void send_buildsSingleMessageWithAllRecipients() throws Exception {
        EmailSender sender = new EmailSender(mailSender, verifier, "noreply@example.com");
        Notification notification = new Notification(
                List.of("a@example.com", "b@example.com"), "Subject", "Body");

        MimeMessage sent = sendAndCapture(sender, notification);

        // One notification must produce exactly one email
        Message.RecipientType to = Message.RecipientType.TO;
        assertEquals(2, sent.getRecipients(to).length);
        assertEquals("a@example.com", sent.getRecipients(to)[0].toString());
        assertEquals("b@example.com", sent.getRecipients(to)[1].toString());
        assertEquals("Subject", sent.getSubject());
        assertEquals("Body", sent.getContent());
        assertEquals("noreply@example.com", sent.getFrom()[0].toString());
    }

    @Test
    void send_withBlankFromAddress_doesNotSetFrom() throws Exception {
        EmailSender sender = new EmailSender(mailSender, verifier, "");
        Notification notification = new Notification(List.of("a@example.com"), "Subject", "Body");

        MimeMessage sent = sendAndCapture(sender, notification);

        assertNull(sent.getFrom());
    }

    @Test
    void send_usesDeterministicMessageId() throws Exception {
        EmailSender sender = new EmailSender(mailSender, verifier, "noreply@example.com");
        Notification notification = new Notification(List.of("a@example.com"), "Subject", "Body");

        MimeMessage sent = sendAndCapture(sender, notification);

        assertEquals("<notification-null@projectnotification.local>", sent.getHeader("Message-ID")[0]);
    }

    @Test
    void send_whenAlreadyDelivered_doesNotSendAgain() {
        when(verifier.wasAlreadyDelivered(any())).thenReturn(true);
        EmailSender sender = new EmailSender(mailSender, verifier, "noreply@example.com");
        Notification notification = new Notification(List.of("a@example.com"), "Subject", "Body");

        assertDoesNotThrow(() -> sender.send(notification));

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void send_withoutRecipients_throwsAndSendsNothing() {
        EmailSender sender = new EmailSender(mailSender, verifier, "noreply@example.com");
        Notification notification = new Notification(List.of(), "Subject", "Body");

        assertThrows(PermanentDeliveryException.class, () -> sender.send(notification));

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void send_whenMailServerFails_throwsRetryable() {
        doThrow(new MailSendException("SMTP down")).when(mailSender).send(any(MimeMessage.class));
        EmailSender sender = new EmailSender(mailSender, verifier, "noreply@example.com");
        Notification notification = new Notification(List.of("a@example.com"), "Subject", "Body");

        assertThrows(RetryableDeliveryException.class, () -> sender.send(notification));
    }

    @Test
    void send_whenAuthenticationFails_throwsPermanent() {
        doThrow(new MailAuthenticationException("bad credentials"))
                .when(mailSender).send(any(MimeMessage.class));
        EmailSender sender = new EmailSender(mailSender, verifier, "noreply@example.com");
        Notification notification = new Notification(List.of("a@example.com"), "Subject", "Body");

        assertThrows(PermanentDeliveryException.class, () -> sender.send(notification));
    }
}