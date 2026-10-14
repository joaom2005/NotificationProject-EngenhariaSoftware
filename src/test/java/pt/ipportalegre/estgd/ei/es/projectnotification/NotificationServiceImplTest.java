package pt.ipportalegre.estgd.ei.es.projectnotification;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pt.ipportalegre.estgd.ei.es.projectnotification.dto.NotificationRequest;
import pt.ipportalegre.estgd.ei.es.projectnotification.entity.Notification;
import pt.ipportalegre.estgd.ei.es.projectnotification.repository.NotificationRepository;
import pt.ipportalegre.estgd.ei.es.projectnotification.sender.NotificationSender;
import pt.ipportalegre.estgd.ei.es.projectnotification.service.NotificationServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationSender emailSender;

    @InjectMocks
    private NotificationServiceImpl service;

    private NotificationRequest buildRequest() {
        NotificationRequest request = new NotificationRequest();
        request.setSubject("Meeting");
        request.setMessage("Tomorrow at 10:00");
        request.setRecipients(List.of("a@example.com", "b@example.com"));
        return request;
    }

    @Test
    void createNotification_savesNotificationWithRequestData() {
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.createNotification(buildRequest());

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertEquals("Meeting", saved.getSubject());
        assertEquals("Tomorrow at 10:00", saved.getMessage());
        assertEquals(List.of("a@example.com", "b@example.com"), saved.getRecipients());
    }

    @Test
    void createNotification_sendsTheSavedNotification() {
        Notification persisted = new Notification(List.of("a@example.com"), "Meeting", "Body");
        when(notificationRepository.save(any(Notification.class))).thenReturn(persisted);

        service.createNotification(buildRequest());

        // The sender must receive the instance returned by the repository
        verify(emailSender).send(persisted);
    }

    @Test
    void createNotification_savesBeforeSending() {
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.createNotification(buildRequest());

        InOrder order = inOrder(notificationRepository, emailSender);
        order.verify(notificationRepository).save(any(Notification.class));
        order.verify(emailSender).send(any(Notification.class));
    }

    @Test
    void createNotification_whenSendingFails_exceptionPropagates() {
        // Documents current behaviour: the notification is already saved and
        // the failure is not handled (failure handling is planned for Phase 2).
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new IllegalStateException("SMTP down"))
                .when(emailSender).send(any(Notification.class));

        assertThrows(IllegalStateException.class,
                () -> service.createNotification(buildRequest()));

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void getNotifications_returnsAllFromRepository() {
        List<Notification> expected = List.of(
                new Notification(List.of("a@example.com"), "One", "Body 1"),
                new Notification(List.of("b@example.com"), "Two", "Body 2"));
        when(notificationRepository.findAll()).thenReturn(expected);

        List<Notification> result = service.getNotifications();

        assertEquals(expected, result);
    }

    @Test
    void getNotification_whenFound_returnsIt() {
        Notification notification = new Notification(List.of("a@example.com"), "One", "Body");
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        assertSame(notification, service.getNotification(1L));
    }

    @Test
    void getNotification_whenMissing_throwsEntityNotFound() {
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> service.getNotification(99L));

        assertEquals("Notification not found: 99", ex.getMessage());
        verify(emailSender, never()).send(any());
    }
}