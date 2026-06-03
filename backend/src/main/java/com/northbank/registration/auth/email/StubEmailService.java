// Story: US-004
package com.northbank.registration.auth.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Stub email service for dev/test profiles.
 * Logs the reset link instead of sending a real email.
 * A real SMTP/SES implementation can replace this without changing callers.
 */
@Slf4j
@Service
@Primary
public class StubEmailService implements EmailService {

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        log.info("[STUB EMAIL] Password reset link for {}: {}", toEmail, resetLink);
    }
}
