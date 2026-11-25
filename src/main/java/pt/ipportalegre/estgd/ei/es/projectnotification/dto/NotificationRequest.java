package pt.ipportalegre.estgd.ei.es.projectnotification.dto;

import jakarta.validation.constraints.*;
import pt.ipportalegre.estgd.ei.es.projectnotification.entity.NotificationChannel;
import pt.ipportalegre.estgd.ei.es.projectnotification.validation.ValidNotificationRequest;

import java.util.ArrayList;
import java.util.List;

@ValidNotificationRequest
public class NotificationRequest {
    @NotBlank(message = "Subject must not be blank")
    @Size(max = 255, message = "Subject must have at most 255 characters")
    private String subject;

    @NotBlank(message = "Message must not be blank")
    private String message;

    private List<String> recipients = new ArrayList<>();

    @NotNull(message = "Select a channel")
    private NotificationChannel channel = NotificationChannel.EMAIL;

    public String getSubject() {
        return subject;
    }
    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getRecipients(){
        return recipients;
    }
    public void setRecipients(List<String> recipients) {
        this.recipients = recipients;
    }

    public NotificationChannel getChannel() {
        return channel;
    }
    public void setChannel(NotificationChannel channel) {
        this.channel = channel;
    }
}
