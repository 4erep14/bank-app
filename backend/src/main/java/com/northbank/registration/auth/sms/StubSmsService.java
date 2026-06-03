// Story: US-003
package com.northbank.registration.auth.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Stub SMS service active on {@code dev} and {@code test} profiles (ADR-003).
 *
 * <p>Logs the OTP at INFO level instead of making a real SMS gateway call.
 * A production implementation can replace this without changing any callers —
 * mirrors the {@code StubEmailService} pattern from ADR-004.</p>
 */
@Slf4j
@Service
@Primary
@Profile({"dev", "test"})
public class StubSmsService implements SmsService {

    @Override
    public void sendOtp(String phoneNumber, String otp) {
        log.info("[STUB SMS] OTP for {}: {}", phoneNumber, otp);
    }
}
