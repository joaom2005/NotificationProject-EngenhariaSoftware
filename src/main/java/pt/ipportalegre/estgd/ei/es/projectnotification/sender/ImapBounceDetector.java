package pt.ipportalegre.estgd.ei.es.projectnotification.sender;

import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.search.HeaderTerm;
import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.internet.ContentType;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import pt.ipportalegre.estgd.ei.es.projectnotification.entity.Notification;

import java.util.Optional;
import java.util.Properties;

/**
 * Looks in the INBOX for a bounce (multipart/report) whose In-Reply-To header is the
 * Message-ID of the notification.
 *
 * <p>Fails open: if the mailbox cannot be read, it reports "no bounce" and the
 * notification is simply checked again on the next run.
 */
@Component
@ConditionalOnProperty(name = "app.delivery.bounce.enabled", havingValue = "true")
public class ImapBounceDetector implements BounceDetector {

    private static final Logger log = LoggerFactory.getLogger(ImapBounceDetector.class);

    private final String host;
    private final String username;
    private final String password;

    public ImapBounceDetector(
            @Value("${app.delivery.verify.imap-host:imap.gmail.com}") String host,
            @Value("${spring.mail.username:}") String username,
            @Value("${spring.mail.password:}") String password) {
        this.host = host;
        this.username = username;
        this.password = password;
    }

    @Override
    public Optional<String> findBounce(Notification notification) {
        String messageId = MessageIds.forNotification(notification);
        Session session = Session.getInstance(new Properties());

        try (Store store = session.getStore("imaps")) {
            store.connect(host, username, password);

            Folder inbox = store.getFolder("INBOX");
            try {
                inbox.open(Folder.READ_ONLY);

                for (Message message : inbox.search(new HeaderTerm("In-Reply-To", messageId))) {
                    String[] replies = message.getHeader("In-Reply-To");
                    if (replies == null || !matchesMessageId(replies, messageId)) {
                        continue;
                    }

                    Optional<String> reason = findPermanentFailure(message);
                    if (reason.isPresent()) {
                        return reason;
                    }
                }
                return Optional.empty();
            } finally {
                if (inbox.isOpen()) {
                    inbox.close(false);
                }
            }
        } catch (Exception e) {
            log.warn("Could not check bounces for notification {}", notification.getId(), e);
            return Optional.empty();
        }
    }

    private static final Pattern FAILED_ACTION =
            Pattern.compile("(?im)^Action:[ \\t]*(?:\\R[ \\t]+)?failed[ \\t]*$");

    private boolean matchesMessageId(String[] replies, String messageId) {
        for (String reply : replies) {
            for (String reference : reply.split("\\s+")) {
                if (messageId.equals(reference)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Optional<String> findPermanentFailure(Message message) throws Exception {
        ContentType type = new ContentType(message.getContentType());
        if (!type.match("multipart/report")
                || !"delivery-status".equalsIgnoreCase(type.getParameter("report-type"))) {
            return Optional.empty();
        }

        if (!(message.getContent() instanceof Multipart report)) {
            return Optional.empty();
        }

        for (int i = 0; i < report.getCount(); i++) {
            BodyPart part = report.getBodyPart(i);
            if (!part.isMimeType("message/delivery-status")) {
                continue;
            }

            String status;
            try (InputStream input = part.getInputStream()) {
                status = new String(input.readAllBytes(), StandardCharsets.US_ASCII);
            }

            String[] recipients = message.getHeader("X-Failed-Recipients");
            String failedRecipients = recipients == null ? "" : String.join(", ", recipients);

            if (FAILED_ACTION.matcher(status).find()) {
                return Optional.of(failedRecipients.isBlank()
                        ? "Permanent delivery failure"
                        : "Rejected recipients: " + failedRecipients);
            }

            if (status.isBlank()
                    && !failedRecipients.isBlank()
                    && "Delivery Status Notification (Failure)"
                    .equalsIgnoreCase(message.getSubject())) {
                return Optional.of("Rejected recipients: " + failedRecipients);
            }
        }

        return Optional.empty();
    }
}