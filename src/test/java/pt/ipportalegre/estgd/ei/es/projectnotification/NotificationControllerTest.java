package pt.ipportalegre.estgd.ei.es.projectnotification;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import pt.ipportalegre.estgd.ei.es.projectnotification.controller.GlobalExceptionHandler;
import pt.ipportalegre.estgd.ei.es.projectnotification.controller.NotificationController;
import pt.ipportalegre.estgd.ei.es.projectnotification.dto.NotificationRequest;
import pt.ipportalegre.estgd.ei.es.projectnotification.entity.Notification;
import pt.ipportalegre.estgd.ei.es.projectnotification.service.NotificationUseCases;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationUseCases notificationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver("/WEB-INF/views/", ".html");

        mockMvc = MockMvcBuilders
                .standaloneSetup(new NotificationController(notificationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setViewResolvers(viewResolver)
                .build();
    }

    @Test
    void home_redirectsToNotifications() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notifications"));
    }

    @Test
    void list_putsNotificationsInModel() throws Exception {
        List<Notification> notifications = List.of(
                new Notification(List.of("a@example.com"), "One", "Body"));
        when(notificationService.getNotifications()).thenReturn(notifications);

        mockMvc.perform(get("/notifications"))
                .andExpect(status().isOk())
                .andExpect(view().name("list"))
                .andExpect(model().attribute("notifications", notifications));
    }

    @Test
    void createForm_showsEmptyForm() throws Exception {
        mockMvc.perform(get("/notifications/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("create"))
                .andExpect(model().attributeExists("notification"));
    }

    @Test
    void create_withValidData_callsServiceAndRedirects() throws Exception {
        mockMvc.perform(post("/notifications")
                        .param("subject", "Meeting")
                        .param("message", "Tomorrow at 10:00")
                        .param("recipients", "a@example.com, b@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notifications"))
                .andExpect(flash().attributeExists("successMessage"));

        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService).createNotification(captor.capture());

        NotificationRequest sent = captor.getValue();
        assertEquals("Meeting", sent.getSubject());
        assertEquals("Tomorrow at 10:00", sent.getMessage());
        assertEquals(List.of("a@example.com", "b@example.com"), sent.getRecipients());
    }

    @Test
    void create_withInvalidEmail_returnsFormWithFieldErrorAndDoesNotCallService() throws Exception {
        mockMvc.perform(post("/notifications")
                        .param("subject", "Meeting")
                        .param("message", "Body")
                        .param("recipients", "not-an-email"))
                .andExpect(status().isOk())
                .andExpect(view().name("create"))
                // Errors on list items are registered per index, e.g. recipients[0]
                .andExpect(model().attributeHasFieldErrors("notification", "recipients[0]"));

        verifyNoInteractions(notificationService);
    }

    @Test
    void create_withBlankSubject_returnsFormAndDoesNotCallService() throws Exception {
        mockMvc.perform(post("/notifications")
                        .param("subject", "")
                        .param("message", "Body")
                        .param("recipients", "a@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("create"))
                .andExpect(model().attributeHasFieldErrors("notification", "subject"));

        verifyNoInteractions(notificationService);
    }

    @Test
    void details_whenFound_putsNotificationInModel() throws Exception {
        Notification notification = new Notification(List.of("a@example.com"), "One", "Body");
        when(notificationService.getNotification(1L)).thenReturn(notification);

        mockMvc.perform(get("/notifications/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("details"))
                .andExpect(model().attribute("notification", notification));
    }

    @Test
    void details_whenMissing_returns404ErrorView() throws Exception {
        when(notificationService.getNotification(99L))
                .thenThrow(new EntityNotFoundException("Notification not found: 99"));

        mockMvc.perform(get("/notifications/99"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("title", "Not found"))
                .andExpect(model().attribute("message", "Notification not found: 99"));
    }
}