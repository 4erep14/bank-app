// Story: US-004
package com.northbank.registration.auth.passwordreset;

import com.northbank.registration.auth.email.EmailService;
import com.northbank.registration.auth.exception.InvalidResetTokenException;
import com.northbank.registration.auth.passwordreset.dto.ForgotPasswordRequest;
import com.northbank.registration.auth.passwordreset.dto.MessageResponse;
import com.northbank.registration.auth.passwordreset.dto.ResetPasswordRequest;
import com.northbank.registration.customer.domain.model.Customer;
import com.northbank.registration.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final int TOKEN_BYTES = 32;
    private static final long TOKEN_EXPIRY_HOURS = 1;
    private static final String UNIFORM_RESPONSE =
            "If an account exists for that email, a password reset link has been sent.";

    private final CustomerRepository customerRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.base-url:http://localhost:3000}")
    private String appBaseUrl;

    /**
     * Initiate password reset (AC1, AC2).
     * Always returns a uniform message to prevent user enumeration.
     */
    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        String email = request.email().toLowerCase().trim();
        Optional<Customer> customerOpt = customerRepository.findByEmail(email);

        if (customerOpt.isPresent()) {
            Customer customer = customerOpt.get();
            tokenRepository.invalidatePriorTokensForCustomer(customer.getId());

            byte[] bytes = new byte[TOKEN_BYTES];
            new SecureRandom().nextBytes(bytes);
            String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            String tokenHash = sha256Hex(rawToken);

            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .customer(customer)
                    .tokenHash(tokenHash)
                    .expiresAt(OffsetDateTime.now().plusHours(TOKEN_EXPIRY_HOURS))
                    .build();
            tokenRepository.save(resetToken);

            String resetLink = appBaseUrl + "/reset-password?token=" + rawToken;
            emailService.sendPasswordResetEmail(email, resetLink);
        }

        return new MessageResponse(UNIFORM_RESPONSE);
    }

    /**
     * Complete password reset (AC3-AC7).
     */
    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        String incomingHash = sha256Hex(request.token());

        PasswordResetToken resetToken = tokenRepository.findByTokenHash(incomingHash)
                .orElseThrow(InvalidResetTokenException::new);

        if (resetToken.isUsed() || resetToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new InvalidResetTokenException();
        }

        Customer customer = resetToken.getCustomer();
        customer.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        customer.setPasswordChangedAt(OffsetDateTime.now());  // AC5: invalidates all existing JWTs
        customerRepository.save(customer);

        resetToken.setUsed(true);  // AC7: single-use
        tokenRepository.save(resetToken);

        log.info("Password reset completed for customer id={}", customer.getId());
        return new MessageResponse("Your password has been reset. Please sign in with your new password.");
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
