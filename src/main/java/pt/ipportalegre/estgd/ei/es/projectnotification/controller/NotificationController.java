package pt.ipportalegre.estgd.ei.es.projectnotification.controller;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import pt.ipportalegre.estgd.ei.es.projectnotification.dto.NotificationRequest;
import pt.ipportalegre.estgd.ei.es.projectnotification.entity.Notification;
import pt.ipportalegre.estgd.ei.es.projectnotification.service.NotificationUseCases;

import java.util.List;

@Controller
public class NotificationController {

        private final NotificationUseCases notificationService;

        public NotificationController(
                NotificationUseCases notificationService) {

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
            model.addAttribute("notification", new NotificationRequest());
            return "create";
        }


        /**
         * Creates and sends a new notification.
         */
        @PostMapping("/notifications")
        public String create(
                @Valid @ModelAttribute("notification") NotificationRequest notification,
                BindingResult result,
                RedirectAttributes redirectAttributes) {

            if (result.hasErrors()) {
                result.getFieldErrors().forEach(e ->
                        System.out.println(e.getField() + " -> " + e.getDefaultMessage()));

                return "create";
            }

            notificationService.createNotification(notification);
            redirectAttributes.addFlashAttribute("successMessage", "Notification created successfully.");
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
