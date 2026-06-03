// Story: US-008
package com.northbank.registration.account.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

/**
 * Exception handlers for account-domain exceptions.
 * Separate advice class keeps account errors isolated from the global handler.
 */
@RestControllerAdvice
@Order(1)
public class AccountExceptionHandler {

    // ── US-006 ─────────────────────────────────────────────────────────────

    @ExceptionHandler(DuplicateAccountTypeException.class)
    public ProblemDetail handleDuplicateAccountType(
            DuplicateAccountTypeException ex, HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Duplicate Account Type");
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    // ── US-008 ─────────────────────────────────────────────────────────────

    @ExceptionHandler(AccountNotFoundException.class)
    public ProblemDetail handleAccountNotFound(
            AccountNotFoundException ex, HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Account Not Found");
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(AccountAccessDeniedException.class)
    public ProblemDetail handleAccountAccessDenied(
            AccountAccessDeniedException ex, HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setTitle("Access Denied");
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }
}
