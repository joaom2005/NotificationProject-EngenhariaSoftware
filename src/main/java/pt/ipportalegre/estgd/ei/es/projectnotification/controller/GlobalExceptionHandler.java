package pt.ipportalegre.estgd.ei.es.projectnotification.controller;

import jakarta.persistence.EntityNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String ERROR_VIEW = "error";

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(EntityNotFoundException ex, Model model) {
        return notFoundView(model, ex.getMessage());
    }

    /** A missing page or static file (e.g. /favicon.ico) is a client error, not a server error. */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleMissingResource(NoResourceFoundException ex, Model model) {
        log.debug("Resource not found: {}", ex.getResourcePath());
        return notFoundView(model, "The page you requested does not exist.");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneric(Exception ex, Model model) {
        // The user sees a generic message; the real cause goes to the log
        log.error("Unhandled exception", ex);
        model.addAttribute("title", "Something went wrong");
        model.addAttribute("message", "An unexpected error occurred.");
        return ERROR_VIEW;
    }

    private String notFoundView(Model model, String message) {
        model.addAttribute("title", "Not found");
        model.addAttribute("message", message);
        return ERROR_VIEW;
    }
}