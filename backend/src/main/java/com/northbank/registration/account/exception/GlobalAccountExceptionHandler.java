// Story: US-008
package com.northbank.registration.account.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

/**
 * Handles account-domain exceptions and maps them to RFC-7807 ProblemDetail responses.
 *
 * <p>If a project-wide GlobalExceptionHandler already exists, merge these
 * {@code @ExceptionHandler} methods into it instead of keeping this class.</p>
 */
@RestControllerAdvice
public class GlobalAccountExceptionHandler {

    /** US-008 AC4: account ID not found → 404 */
    @ExceptionHandler(AccountNotFoundException.class)
    public ProblemDetail handleAccountNotFound(
            AccountNotFoundException ex, HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail
                .forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Account Not Found");
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    /** US-008 AC3: account belongs to a different customer → 403 */
    @ExceptionHandler(AccountAccessDeniedException.class)
    public ProblemDetail handleAccountAccessDenied(
            AccountAccessDeniedException ex, HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail
                .forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setTitle("Access Denied");
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    /** US-006 AC5: duplicate account type → 409 */
    @ExceptionHandler(DuplicateAccountTypeException.class)
    public ProblemDetail handleDuplicateAccountType(
            DuplicateAccountTypeException ex, HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail
                .forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Duplicate Account Type");
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }
}
