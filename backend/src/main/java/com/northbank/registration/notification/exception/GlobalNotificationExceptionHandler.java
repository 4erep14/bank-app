// Story: US-016
package com.northbank.registration.notification.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
public class GlobalNotificationExceptionHandler {

    @ExceptionHandler(NotificationAccessDeniedException.class)
    public ProblemDetail handleAccessDenied(NotificationAccessDeniedException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setTitle("Notification Access Denied");
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }
}
