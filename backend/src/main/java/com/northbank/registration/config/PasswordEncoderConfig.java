// Story: US-001
package com.northbank.registration.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Provides the BCrypt {@link PasswordEncoder} bean used by {@code CustomerService}
 * to hash passwords before persistence.
 *
 * <p>Strength 12 is used per ADR-001: balances security against registration
 * latency (~300 ms on modern hardware).</p>
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * BCrypt password encoder with strength (cost factor) 12.
     * Produced hashes are 60 characters — fits in the {@code VARCHAR(72)}
     * {@code password_hash} column with room to spare.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
