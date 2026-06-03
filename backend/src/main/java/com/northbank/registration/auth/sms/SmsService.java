// Story: US-003
package com.northbank.registration.auth.sms;

/**
 * Abstraction over SMS delivery for OTP codes (ADR-003).
 *
 * <p>The single production implementation will call the real SMS gateway.
 * {@link StubSmsService} is active on {@code dev} and {@code test} profiles,
 * logging the OTP instead of sending it — mirrors the ADR-004 EmailService pattern.</p>
 */
public interface SmsService {

    /**
     * Sends a one-time passcode to the given phone number.
     *
     * @param phoneNumber the recipient's phone number (E.164 or local format)
     * @param otp         the 6-digit OTP string to deliver
     */
    void sendOtp(String phoneNumber, String otp);
}
