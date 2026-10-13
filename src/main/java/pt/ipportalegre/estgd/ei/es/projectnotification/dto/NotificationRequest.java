package pt.ipportalegre.estgd.ei.es.projectnotification.dto;

import jakarta.validation.constraints.*;

import java.util.ArrayList;
import java.util.List;

public class NotificationRequest {
    @NotBlank(message = "Subject must not be blank")
    @Size(max = 255, message = "Subject must have at most 255 characters")
    private String subject;

    @NotBlank(message = "Message must not be blank")
    private String message;

    @NotEmpty(message = "Enter at least one recipient")
    private List<@NotBlank(message = "Empty email address") @Email(message = "Invalid email address") String> recipients = new ArrayList<>();

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

    public List<@NotBlank(message = "Empty email address") @Email(message = "Invalid email address") String> getRecipients(){
        return recipients;
    }

    public void setRecipients(List<@NotBlank(message = "Empty email address") @Email(message = "Invalid email address") String> recipients) {
        this.recipients = recipients;
    }
}
