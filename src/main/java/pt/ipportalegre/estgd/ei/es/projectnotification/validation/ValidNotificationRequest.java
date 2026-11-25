package pt.ipportalegre.estgd.ei.es.projectnotification.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NotificationRequestValidator.class)
public @interface ValidNotificationRequest {
    String message() default "Invalid notification request";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}