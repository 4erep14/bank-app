// Story: US-002 / US-003
package com.northbank.registration.auth.login;

import com.northbank.registration.auth.login.dto.LoginRequest;
import com.northbank.registration.auth.login.dto.LoginResponse;
import com.northbank.registration.auth.login.exception.AccountLockedException;
import com.northbank.registration.auth.login.exception.InvalidCredentialsException;
import com.northbank.registration.auth.otp.OtpService;
import com.northbank.registration.config.JwtConfig;
import com.northbank.registration.customer.domain.model.Customer;
import com.northbank.registration.customer.domain.model.CustomerStatus;
import com.northbank.registration.customer.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AuthService}.
 * Uses Mockito — no Spring context, no database.
 *
 * <p>Acceptance Criteria covered:</p>
 * <ul>
 *   <li>AC2 — valid credentials → 200 with "2FA_REQUIRED" + sessionToken</li>
 *   <li>AC3 — invalid credentials → {@link InvalidCredentialsException}</li>
 *   <li>AC4 — 5th failure → account LOCKED, {@link AccountLockedException}</li>
 *   <li>AC5 — already LOCKED → {@link AccountLockedException} immediately</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService unit tests — US-002")
class AuthServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtConfig jwtConfig;

    /**
     * US-003 additive dependency: OtpService is called on every successful login
     * to create the OTP session. Mock it here so unit tests do not require a
     * running database or SMS stub.
     */
    @Mock
    private OtpService otpService;

    @InjectMocks
    private AuthService authService;

    private static final String RAW_PASSWORD = "Str0ng!Pass";
    private static final String HASHED_PASSWORD = "$2a$12$hashedpassword";
    private static final String EMAIL = "ada.lovelace@example.com";

    // ── helpers ───────────────────────────────────────────────────────────────

    private Customer activeCustomer(int failedAttempts) {
        return Customer.builder()
                .id(UUID.randomUUID())
                .firstName("Ada").lastName("Lovelace")
                .email(EMAIL)
                .phoneNumber("+14155552671")
                .dateOfBirth(LocalDate.of(1990, 12, 10))
                .passwordHash(HASHED_PASSWORD)
                .status(CustomerStatus.ACTIVE)
                .failedLoginAttempts(failedAttempts)
                .build();
    }

    private Customer lockedCustomer() {
        return Customer.builder()
                .id(UUID.randomUUID())
                .firstName("Ada").lastName("Lovelace")
                .email(EMAIL)
                .phoneNumber("+14155552671")
                .dateOfBirth(LocalDate.of(1990, 12, 10))
                .passwordHash(HASHED_PASSWORD)
                .status(CustomerStatus.LOCKED)
                .failedLoginAttempts(5)
                .build();
    }

    private LoginRequest loginRequest() {
        return new LoginRequest(EMAIL, RAW_PASSWORD);
    }

    // ─── AC2 — Valid credentials ──────────────────────────────────────────────

    @Test
    @DisplayName("AC2 — valid credentials: returns LoginResponse with 2FA_REQUIRED and sessionToken")
    void login_validCredentials_returnsSessionToken() {
        Customer customer = activeCustomer(0);
        when(customerRepository.findByEmail(EMAIL)).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(true);
        when(jwtConfig.generateSessionToken(customer.getId())).thenReturn("signed.jwt.token");
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        LoginResponse response = authService.login(loginRequest());

        assertThat(response.status()).isEqualTo("2FA_REQUIRED");
        assertThat(response.sessionToken()).isEqualTo("signed.jwt.token");
    }

    @Test
    @DisplayName("AC2 — valid credentials: failedLoginAttempts reset to 0")
    void login_validCredentials_resetsFailedAttempts() {
        Customer customer = activeCustomer(3);  // had 3 prior failures
        when(customerRepository.findByEmail(EMAIL)).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(true);
        when(jwtConfig.generateSessionToken(any())).thenReturn("token");
        when(customerRepository.save(any())).thenReturn(customer);

        authService.login(loginRequest());

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(captor.capture());
        assertThat(captor.getValue().getFailedLoginAttempts()).isZero();
    }

    @Test
    @DisplayName("AC2 — email is normalised to lowercase before lookup")
    void login_emailNormalisedToLowercase() {
        Customer customer = activeCustomer(0);
        when(customerRepository.findByEmail("ada.lovelace@example.com")).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);
        when(jwtConfig.generateSessionToken(any())).thenReturn("token");
        when(customerRepository.save(any())).thenReturn(customer);

        authService.login(new LoginRequest("Ada.Lovelace@Example.COM", RAW_PASSWORD));

        verify(customerRepository).findByEmail("ada.lovelace@example.com");
    }

    // ─── AC3 — Unknown email ──────────────────────────────────────────────────

    @Test
    @DisplayName("AC3 — unknown email: throws InvalidCredentialsException (no user enumeration)")
    void login_unknownEmail_throwsInvalidCredentialsException() {
        when(customerRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest()))
                .isInstanceOf(InvalidCredentialsException.class);

        verifyNoInteractions(passwordEncoder, jwtConfig);
    }

    // ─── AC3 — Wrong password ─────────────────────────────────────────────────

    @Test
    @DisplayName("AC3 — wrong password: throws InvalidCredentialsException")
    void login_wrongPassword_throwsInvalidCredentialsException() {
        Customer customer = activeCustomer(0);
        when(customerRepository.findByEmail(EMAIL)).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(false);
        when(customerRepository.save(any())).thenReturn(customer);

        assertThatThrownBy(() -> authService.login(loginRequest()))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("AC3 — wrong password: failedLoginAttempts incremented (from 0 to 1)")
    void login_wrongPassword_incrementsFailedAttempts() {
        Customer customer = activeCustomer(0);
        when(customerRepository.findByEmail(EMAIL)).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(false);
        when(customerRepository.save(any())).thenReturn(customer);

        assertThatThrownBy(() -> authService.login(loginRequest()))
                .isInstanceOf(InvalidCredentialsException.class);

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(captor.capture());
        assertThat(captor.getValue().getFailedLoginAttempts()).isEqualTo(1);
    }

    // ─── AC4 — 5th consecutive failure triggers lockout ──────────────────────

    @Test
    @DisplayName("AC4 — 5th consecutive failure: throws AccountLockedException")
    void login_fifthFailure_throwsAccountLockedException() {
        Customer customer = activeCustomer(4);   // 4 prior failures; this is the 5th
        when(customerRepository.findByEmail(EMAIL)).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(false);
        when(customerRepository.save(any())).thenReturn(customer);

        assertThatThrownBy(() -> authService.login(loginRequest()))
                .isInstanceOf(AccountLockedException.class);
    }

    @Test
    @DisplayName("AC4 — 5th failure: status set to LOCKED, lockedAt set")
    void login_fifthFailure_setsStatusLockedAndLockedAt() {
        Customer customer = activeCustomer(4);
        when(customerRepository.findByEmail(EMAIL)).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(false);
        when(customerRepository.save(any())).thenReturn(customer);

        assertThatThrownBy(() -> authService.login(loginRequest()))
                .isInstanceOf(AccountLockedException.class);

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(captor.capture());

        Customer saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(CustomerStatus.LOCKED);
        assertThat(saved.getLockedAt()).isNotNull();
        assertThat(saved.getFailedLoginAttempts()).isEqualTo(5);
    }

    @Test
    @DisplayName("AC4 — 4th failure (below threshold): still throws InvalidCredentialsException, not locked")
    void login_fourthFailure_notYetLocked_throwsInvalidCredentials() {
        Customer customer = activeCustomer(3);   // 3 prior; 4th attempt
        when(customerRepository.findByEmail(EMAIL)).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(false);
        when(customerRepository.save(any())).thenReturn(customer);

        assertThatThrownBy(() -> authService.login(loginRequest()))
                .isInstanceOf(InvalidCredentialsException.class);

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(captor.getValue().getLockedAt()).isNull();
    }

    // ─── AC5 — Already LOCKED account ────────────────────────────────────────

    @Test
    @DisplayName("AC5 — already LOCKED account: throws AccountLockedException immediately")
    void login_lockedAccount_throwsAccountLockedException() {
        Customer customer = lockedCustomer();
        when(customerRepository.findByEmail(EMAIL)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> authService.login(loginRequest()))
                .isInstanceOf(AccountLockedException.class);

        // Password check must NOT be attempted on a locked account
        verifyNoInteractions(passwordEncoder, jwtConfig);
    }

    @Test
    @DisplayName("AC5 — LOCKED account: no DB write occurs (counter not touched)")
    void login_lockedAccount_noSavePerformed() {
        Customer customer = lockedCustomer();
        when(customerRepository.findByEmail(EMAIL)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> authService.login(loginRequest()))
                .isInstanceOf(AccountLockedException.class);

        verify(customerRepository, never()).save(any());
    }
}
