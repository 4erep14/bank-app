// Story: US-003
package com.northbank.registration.auth.otp.exception;

/**
 * Thrown when a resend-OTP request is made within 60 seconds of the last
 * OTP being issued for the same session.
 *
 * <p>Maps to HTTP 429 Too Many Requests via {@code GlobalExceptionHandler},
 * with a {@code Retry-After: 60} response header.</p>
 */
public class TooManyOtpRequestsException extends RuntimeException {

    public TooManyOtpRequestsException() {
        super("OTP was sent recently. Please wait 60 seconds before requesting a new code.");
    }
}
