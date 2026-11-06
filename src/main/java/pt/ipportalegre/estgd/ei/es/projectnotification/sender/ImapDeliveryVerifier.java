package pt.ipportalegre.estgd.ei.es.projectnotification.sender;

import jakarta.mail.Folder;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.search.HeaderTerm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import pt.ipportalegre.estgd.ei.es.projectnotification.entity.Notification;

import java.util.Properties;

/**
 * Looks for the notification's Message-ID in the sent folder of the mailbox over IMAP.
 *
 * <p>If the mailbox cannot be checked, this fails open: it reports "not delivered" and
 * lets the sender try again. A possible duplicate is preferred to a lost notification.
 */
@Component
@ConditionalOnProperty(name = "app.delivery.verify.enabled", havingValue = "true")
public class ImapDeliveryVerifier implements DeliveryVerifier {

    private static final Logger log = LoggerFactory.getLogger(ImapDeliveryVerifier.class);

    private final String host;
    private final String username;
    private final String password;
    private final String sentFolderName;

    public ImapDeliveryVerifier(
            @Value("${app.delivery.verify.imap-host:imap.gmail.com}") String host,
            @Value("${spring.mail.username:}") String username,
            @Value("${spring.mail.password:}") String password,
            @Value("${app.delivery.verify.sent-folder:[Gmail]/Sent Mail}") String sentFolderName) {
        this.host = host;
        this.username = username;
        this.password = password;
        this.sentFolderName = sentFolderName;
    }

    @Override
    public boolean wasAlreadyDelivered(Notification notification) {
        String messageId = MessageIds.forNotification(notification);
        Session session = Session.getInstance(new Properties());

        try (Store store = session.getStore("imaps")) {
            store.connect(host, username, password);

            Folder folder = store.getFolder(sentFolderName);
            try {
                folder.open(Folder.READ_ONLY);
                return folder.search(new HeaderTerm("Message-ID", messageId)).length > 0;
            } finally {
                if (folder.isOpen()) {
                    folder.close(false);
                }
            }
        } catch (Exception e) {
            log.warn("Could not verify delivery of notification {}, assuming not delivered",
                    notification.getId(), e);
            return false;
        }
    }
}