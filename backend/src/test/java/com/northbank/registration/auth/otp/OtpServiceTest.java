// Story: US-003
package com.northbank.registration.auth.otp;

import com.northbank.registration.auth.otp.exception.InvalidOtpException;
import com.northbank.registration.auth.otp.exception.InvalidSessionTokenException;
import com.northbank.registration.auth.otp.exception.OtpSessionInvalidatedException;
import com.northbank.registration.auth.otp.exception.TooManyOtpRequestsException;
import com.northbank.registration.auth.sms.SmsService;
import com.northbank.registration.auth.token.RefreshToken;
import com.northbank.registration.auth.token.RefreshTokenRepository;
import com.northbank.registration.config.JwtConfig;
import com.northbank.registration.customer.domain.model.Customer;
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

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for {@link OtpService} (US-003).
 *
 * <p>All Spring context loading is avoided — pure Mockito-driven unit tests.
 * Covered acceptance criteria:</p>
 * <ul>
 *   <li>AC1 — createOtpSession generates + saves + sends OTP</li>
 *   <li>AC2 — expired OTP session returns 401 "Invalid or expired OTP"</li>
 *   <li>AC3 — valid OTP returns accessToken + refreshToken</li>
 *   <li>AC4 — wrong OTP → remainingAttempts decremented in 401</li>
 *   <li>AC5 — 3 consecutive failures → session invalidated; further attempt → 401</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock private OtpSessionRepository  otpSessionRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtConfig             jwtConfig;
    @Mock private SmsService            smsService;
    @Mock private CustomerRepository    customerRepository;
    @Mock private PasswordEncoder       passwordEncoder;

    @InjectMocks
    private OtpService otpService;

    private static final UUID   CUSTOMER_ID    = UUID.randomUUID();
    private static final String RAW_SESSION    = "raw.session.token";
    private static final String VALID_OTP      = "123456";

    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
                .id(CUSTOMER_ID)
                .firstName("Ada")
                .lastName("Lovelace")
                .email("ada@example.com")
                .phoneNumber("+44700000001")
                .passwordHash("hash")
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AC1 — createOtpSession
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("AC1 — createOtpSession saves a new OTP session and sends SMS")
    void ac1_createOtpSession_savesSessionAndSendsSms() {
        given(customerRepository.findById(CUSTOMER_ID)).willReturn(Optional.of(customer));
        given(otpSessionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        otpService.createOtpSession(CUSTOMER_ID, RAW_SESSION);

        ArgumentCaptor<OtpSession> captor = ArgumentCaptor.forClass(OtpSession.class);
        then(otpSessionRepository).should().save(captor.capture());
        OtpSession saved = captor.getValue();

        assertThat(saved.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(saved.getOtpCode()).matches("\\d{6}");
        assertThat(saved.isInvalidated()).isFalse();
        assertThat(saved.getFailedAttempts()).isZero();
        assertThat(saved.getExpiresAt()).isAfter(OffsetDateTime.now());

        then(smsService).should().sendOtp(eq("+44700000001"), argThat(otp -> otp.matches("\\d{6}")));
    }

    @Test
    @DisplayName("AC1 — createOtpSession hashes the session token (does not store raw JWT)")
    void ac1_createOtpSession_storesHashNotRawToken() {
        given(customerRepository.findById(CUSTOMER_ID)).willReturn(Optional.of(customer));
        given(otpSessionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        otpService.createOtpSession(CUSTOMER_ID, RAW_SESSION);

        ArgumentCaptor<OtpSession> captor = ArgumentCaptor.forClass(OtpSession.class);
        then(otpSessionRepository).should().save(captor.capture());

        String storedHash = captor.getValue().getSessionTokenHash();
        // SHA-256 produces exactly 64 hex characters
        assertThat(storedHash).hasSize(64).matches("[0-9a-f]+");
        // The raw token must never equal the hash
        assertThat(storedHash).isNotEqualTo(RAW_SESSION);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AC2 — expired OTP
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("AC2 — verifyOtp on expired session returns 401 with remainingAttempts")
    void ac2_verifyOtp_expiredSession_throwsInvalidOtpException() {
        given(jwtConfig.validateSessionToken(RAW_SESSION)).willReturn(CUSTOMER_ID);
        OtpSession expired = buildSession(VALID_OTP, OffsetDateTime.now().minusMinutes(1), 1, false);
        given(otpSessionRepository.findBySessionTokenHash(anyString())).willReturn(Optional.of(expired));

        assertThatThrownBy(() -> otpService.verifyOtp(RAW_SESSION, VALID_OTP))
                .isInstanceOf(InvalidOtpException.class)
                .satisfies(ex -> assertThat(((InvalidOtpException) ex).getRemainingAttempts()).isEqualTo(2));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AC3 — valid OTP
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("AC3 — verifyOtp with correct OTP returns accessToken and refreshToken")
    void ac3_verifyOtp_validOtp_returnsTokenPair() {
        given(jwtConfig.validateSessionToken(RAW_SESSION)).willReturn(CUSTOMER_ID);
        given(jwtConfig.generateAccessToken(CUSTOMER_ID)).willReturn("access.token.value");
        OtpSession session = buildSession(VALID_OTP, OffsetDateTime.now().plusMinutes(5), 0, false);
        given(otpSessionRepository.findBySessionTokenHash(anyString())).willReturn(Optional.of(session));
        given(refreshTokenRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(otpSessionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        var response = otpService.verifyOtp(RAW_SESSION, VALID_OTP);

        assertThat(response.accessToken()).isEqualTo("access.token.value");
        assertThat(response.refreshToken()).isNotBlank().hasSize(43); // 32 bytes Base64URL no-pad
    }

    @Test
    @DisplayName("AC3 — verifyOtp marks session invalidated after success (replay prevention)")
    void ac3_verifyOtp_validOtp_marksSessionInvalidated() {
        given(jwtConfig.validateSessionToken(RAW_SESSION)).willReturn(CUSTOMER_ID);
        given(jwtConfig.generateAccessToken(CUSTOMER_ID)).willReturn("token");
        OtpSession session = buildSession(VALID_OTP, OffsetDateTime.now().plusMinutes(5), 0, false);
        given(otpSessionRepository.findBySessionTokenHash(anyString())).willReturn(Optional.of(session));
        given(refreshTokenRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(otpSessionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        otpService.verifyOtp(RAW_SESSION, VALID_OTP);

        assertThat(session.isInvalidated()).isTrue();
    }

    @Test
    @DisplayName("AC3 — verifyOtp persists a refresh token row")
    void ac3_verifyOtp_persistsRefreshToken() {
        given(jwtConfig.validateSessionToken(RAW_SESSION)).willReturn(CUSTOMER_ID);
        given(jwtConfig.generateAccessToken(CUSTOMER_ID)).willReturn("token");
        OtpSession session = buildSession(VALID_OTP, OffsetDateTime.now().plusMinutes(5), 0, false);
        given(otpSessionRepository.findBySessionTokenHash(anyString())).willReturn(Optional.of(session));
        given(refreshTokenRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(otpSessionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        otpService.verifyOtp(RAW_SESSION, VALID_OTP);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        then(refreshTokenRepository).should().save(captor.capture());
        RefreshToken saved = captor.getValue();
        assertThat(saved.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(saved.getTokenHash()).hasSize(64);
        assertThat(saved.isRevoked()).isFalse();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AC4 — wrong OTP; remainingAttempts tracked
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("AC4 — wrong OTP increments failedAttempts and returns remainingAttempts=2")
    void ac4_verifyOtp_wrongOtp_firstFailure() {
        given(jwtConfig.validateSessionToken(RAW_SESSION)).willReturn(CUSTOMER_ID);
        OtpSession session = buildSession("999999", OffsetDateTime.now().plusMinutes(5), 0, false);
        given(otpSessionRepository.findBySessionTokenHash(anyString())).willReturn(Optional.of(session));
        given(otpSessionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> otpService.verifyOtp(RAW_SESSION, "000000"))
                .isInstanceOf(InvalidOtpException.class)
                .satisfies(ex -> assertThat(((InvalidOtpException) ex).getRemainingAttempts()).isEqualTo(2));

        assertThat(session.getFailedAttempts()).isEqualTo(1);
        assertThat(session.isInvalidated()).isFalse();
    }

    @Test
    @DisplayName("AC4 — wrong OTP on second failure returns remainingAttempts=1")
    void ac4_verifyOtp_wrongOtp_secondFailure() {
        given(jwtConfig.validateSessionToken(RAW_SESSION)).willReturn(CUSTOMER_ID);
        OtpSession session = buildSession("999999", OffsetDateTime.now().plusMinutes(5), 1, false);
        given(otpSessionRepository.findBySessionTokenHash(anyString())).willReturn(Optional.of(session));
        given(otpSessionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> otpService.verifyOtp(RAW_SESSION, "000000"))
                .isInstanceOf(InvalidOtpException.class)
                .satisfies(ex -> assertThat(((InvalidOtpException) ex).getRemainingAttempts()).isEqualTo(1));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AC5 — 3 failures → session invalidated
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("AC5 — 3rd wrong OTP invalidates the session and returns remainingAttempts=0")
    void ac5_verifyOtp_thirdFailure_invalidatesSession() {
        given(jwtConfig.validateSessionToken(RAW_SESSION)).willReturn(CUSTOMER_ID);
        OtpSession session = buildSession("999999", OffsetDateTime.now().plusMinutes(5), 2, false);
        given(otpSessionRepository.findBySessionTokenHash(anyString())).willReturn(Optional.of(session));
        given(otpSessionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> otpService.verifyOtp(RAW_SESSION, "000000"))
                .isInstanceOf(InvalidOtpException.class)
                .satisfies(ex -> assertThat(((InvalidOtpException) ex).getRemainingAttempts()).isZero());

        assertThat(session.isInvalidated()).isTrue();
        assertThat(session.getFailedAttempts()).isEqualTo(3);
    }

    @Test
    @DisplayName("AC5 — attempt after session invalidated throws OtpSessionInvalidatedException")
    void ac5_verifyOtp_afterInvalidation_throwsSessionInvalidated() {
        given(jwtConfig.validateSessionToken(RAW_SESSION)).willReturn(CUSTOMER_ID);
        OtpSession session = buildSession(VALID_OTP, OffsetDateTime.now().plusMinutes(5), 3, true);
        given(otpSessionRepository.findBySessionTokenHash(anyString())).willReturn(Optional.of(session));

        assertThatThrownBy(() -> otpService.verifyOtp(RAW_SESSION, VALID_OTP))
                .isInstanceOf(OtpSessionInvalidatedException.class);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JWT validation — invalid token
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("verifyOtp with invalid JWT token throws InvalidSessionTokenException (→ 401)")
    void verifyOtp_invalidJwt_throws401() {
        given(jwtConfig.validateSessionToken(RAW_SESSION))
                .willThrow(new InvalidSessionTokenException());

        assertThatThrownBy(() -> otpService.verifyOtp(RAW_SESSION, VALID_OTP))
                .isInstanceOf(InvalidSessionTokenException.class);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // resendOtp — rate limiting
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("resendOtp within 60 s cool-down throws TooManyOtpRequestsException (→ 429)")
    void resendOtp_withinCoolDown_throws429() {
        given(jwtConfig.validateSessionToken(RAW_SESSION)).willReturn(CUSTOMER_ID);
        OtpSession session = buildSession(VALID_OTP, OffsetDateTime.now().plusMinutes(5), 0, false);
        session.setCreatedAt(OffsetDateTime.now().minusSeconds(30)); // only 30 s old
        given(otpSessionRepository.findBySessionTokenHash(anyString())).willReturn(Optional.of(session));

        assertThatThrownBy(() -> otpService.resendOtp(RAW_SESSION))
                .isInstanceOf(TooManyOtpRequestsException.class);
    }

    @Test
    @DisplayName("resendOtp after cool-down resets OTP and sends SMS")
    void resendOtp_afterCoolDown_sendsNewOtp() {
        given(jwtConfig.validateSessionToken(RAW_SESSION)).willReturn(CUSTOMER_ID);
        OtpSession session = buildSession("111111", OffsetDateTime.now().plusMinutes(5), 1, false);
        session.setCreatedAt(OffsetDateTime.now().minusSeconds(90)); // 90 s old — past cool-down
        given(otpSessionRepository.findBySessionTokenHash(anyString())).willReturn(Optional.of(session));
        given(otpSessionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(customerRepository.findById(CUSTOMER_ID)).willReturn(Optional.of(customer));

        var response = otpService.resendOtp(RAW_SESSION);

        assertThat(response.message()).isEqualTo("OTP sent");
        assertThat(session.getFailedAttempts()).isZero();
        assertThat(session.getOtpCode()).matches("\\d{6}");
        then(smsService).should().sendOtp(eq("+44700000001"), argThat(otp -> otp.matches("\\d{6}")));
    }

    @Test
    @DisplayName("resendOtp on invalidated session throws OtpSessionInvalidatedException")
    void resendOtp_invalidatedSession_throws401() {
        given(jwtConfig.validateSessionToken(RAW_SESSION)).willReturn(CUSTOMER_ID);
        OtpSession session = buildSession(VALID_OTP, OffsetDateTime.now().plusMinutes(5), 3, true);
        given(otpSessionRepository.findBySessionTokenHash(anyString())).willReturn(Optional.of(session));

        assertThatThrownBy(() -> otpService.resendOtp(RAW_SESSION))
                .isInstanceOf(OtpSessionInvalidatedException.class);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────────────────────

    private OtpSession buildSession(String otp, OffsetDateTime expiresAt,
                                    int failedAttempts, boolean invalidated) {
        return OtpSession.builder()
                .id(UUID.randomUUID())
                .customerId(CUSTOMER_ID)
                .sessionTokenHash("somehash")
                .otpCode(otp)
                .expiresAt(expiresAt)
                .failedAttempts(failedAttempts)
                .invalidated(invalidated)
                .createdAt(OffsetDateTime.now().minusSeconds(120))
                .build();
    }
}
