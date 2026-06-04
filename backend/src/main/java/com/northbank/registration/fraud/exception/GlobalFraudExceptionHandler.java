// Story: US-014
package com.northbank.registration.fraud.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
public class GlobalFraudExceptionHandler {

    @ExceptionHandler(FraudRuleNotFoundException.class)
    public ProblemDetail handleRuleNotFound(FraudRuleNotFoundException ex, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "Fraud Rule Not Found", ex.getMessage(), request);
    }

    @ExceptionHandler(FraudAlertNotFoundException.class)
    public ProblemDetail handleAlertNotFound(FraudAlertNotFoundException ex, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "Fraud Alert Not Found", ex.getMessage(), request);
    }

    @ExceptionHandler(DuplicateFraudRuleNameException.class)
    public ProblemDetail handleDuplicateRule(DuplicateFraudRuleNameException ex, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "Duplicate Fraud Rule", ex.getMessage(), request);
    }

    @ExceptionHandler(LastActiveFraudRuleException.class)
    public ProblemDetail handleLastActiveRule(LastActiveFraudRuleException ex, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "Active Rule Required", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidFraudRuleThresholdException.class)
    public ProblemDetail handleInvalidThreshold(InvalidFraudRuleThresholdException ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid Fraud Rule Threshold", ex.getMessage(), request);
    }

    @ExceptionHandler(TransactionAlreadyResolvedException.class)
    public ProblemDetail handleAlreadyResolved(TransactionAlreadyResolvedException ex, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "Transaction Already Resolved", ex.getMessage(), request);
    }

    @ExceptionHandler(InsufficientFundsAtUnblockException.class)
    public ProblemDetail handleInsufficientFundsAtUnblock(InsufficientFundsAtUnblockException ex, HttpServletRequest request) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Insufficient Funds", ex.getMessage(), request);
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }
}
