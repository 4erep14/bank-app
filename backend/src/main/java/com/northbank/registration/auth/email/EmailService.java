// Story: US-004
package com.northbank.registration.auth.email;

public interface EmailService {
    void sendPasswordResetEmail(String toEmail, String resetLink);
}
