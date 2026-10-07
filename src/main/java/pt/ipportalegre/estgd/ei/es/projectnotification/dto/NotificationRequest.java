package pt.ipportalegre.estgd.ei.es.projectnotification.dto;

import java.util.ArrayList;
import java.util.List;

public class NotificationRequest {

    private String subject;
    private String message;
    private List<Long> recipientIds = new ArrayList<>();

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

    public List<Long> getRecipientIds() {
        return recipientIds;
    }

    public void setRecipientIds(List<Long> recipientIds) {
        this.recipientIds = recipientIds;
    }
}
