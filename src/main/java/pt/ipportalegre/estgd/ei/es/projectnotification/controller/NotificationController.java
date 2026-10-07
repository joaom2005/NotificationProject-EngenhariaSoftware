package pt.ipportalegre.estgd.ei.es.projectnotification.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import pt.ipportalegre.estgd.ei.es.projectnotification.dto.NotificationRequest;
import pt.ipportalegre.estgd.ei.es.projectnotification.entity.Notification;
import pt.ipportalegre.estgd.ei.es.projectnotification.entity.Recipient;
import pt.ipportalegre.estgd.ei.es.projectnotification.service.INotificationUseCases;

import java.util.List;

@Controller
public class NotificationController {

        private final INotificationUseCases notificationService;

        public NotificationController(
                INotificationUseCases notificationService) {

            this.notificationService = notificationService;
        }

        @GetMapping("/")
        public String home() {
            return "redirect:/notifications";
        }


        /**
         * Displays all existing notifications.
         */
        @GetMapping("/notifications")
        public String list(Model model) {

            List<Notification> notifications =
                    notificationService.getNotifications();

            model.addAttribute("notifications", notifications);

            return "list";
        }


        /**
         * Displays the notification creation form.
         */
        @GetMapping("/notifications/create")
        public String createForm(Model model) {

            model.addAttribute(
                    "notification",
                    new NotificationRequest()
            );

            List<Recipient> recipients =
                    notificationService.getRecipients();

            model.addAttribute("recipients", recipients);

            return "create";
        }


        /**
         * Creates and sends a new notification.
         */
        @PostMapping("/notifications")
        public String create(
                @ModelAttribute("notification")
                NotificationRequest notification) {

            notificationService.createNotification(notification);

            return "redirect:/notifications";
        }


        /**
         * Displays the details of a single notification.
         */
        @GetMapping("/notifications/{id}")
        public String details(
                @PathVariable Long id,
                Model model) {

            Notification notification =
                    notificationService.getNotification(id);

            model.addAttribute(
                    "notification",
                    notification
            );

            return "details";
        }
    }
