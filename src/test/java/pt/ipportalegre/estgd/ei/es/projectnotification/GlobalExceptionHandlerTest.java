package pt.ipportalegre.estgd.ei.es.projectnotification;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import pt.ipportalegre.estgd.ei.es.projectnotification.controller.GlobalExceptionHandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void entityNotFound_showsItsMessage() {
        Model model = new ConcurrentModel();

        String view = handler.handleNotFound(new EntityNotFoundException("Notification not found: 7"), model);

        assertEquals("error", view);
        assertEquals("Not found", model.getAttribute("title"));
        assertEquals("Notification not found: 7", model.getAttribute("message"));
    }

    @Test
    void missingResource_isTreatedAsNotFound() {
        Model model = new ConcurrentModel();
        NoResourceFoundException missing = mock(NoResourceFoundException.class);

        String view = handler.handleMissingResource(missing, model);

        assertEquals("error", view);
        assertEquals("Not found", model.getAttribute("title"));
    }

    @Test
    void unexpectedException_hidesInternalDetailsFromTheUser() {
        Model model = new ConcurrentModel();

        String view = handler.handleGeneric(new IllegalStateException("secret internals"), model);

        assertEquals("error", view);
        assertEquals("Something went wrong", model.getAttribute("title"));
        assertEquals("An unexpected error occurred.", model.getAttribute("message"));
    }
}