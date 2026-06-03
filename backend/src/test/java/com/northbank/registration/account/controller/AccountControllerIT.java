// Story: US-006
package com.northbank.registration.account.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.northbank.registration.account.domain.model.AccountType;
import com.northbank.registration.account.repository.AccountRepository;
import com.northbank.registration.account.service.dto.OpenAccountRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for US-006: Open a New Bank Account.
 *
 * <p>All 6 Acceptance Criteria are covered:
 * <ul>
 *   <li>AC1 — Authenticated customer can call POST /api/v1/accounts with CHECKING or SAVINGS</li>
 *   <li>AC2 — A unique 10-digit numeric account number is generated and stored</li>
 *   <li>AC3 — Account is created with balance=0.00 and status=ACTIVE</li>
 *   <li>AC4 — API returns 201 with id, accountNumber, type, balance, status, createdAt</li>
 *   <li>AC5 — Duplicate type for same customer returns 409 with spec message</li>
 *   <li>AC6 — Unauthenticated requests return 401</li>
 * </ul>
 * Each test rolls back via {@code @Transactional}.
 * JWT is injected via Spring Security Test's {@code jwt()} post-processor.
 * </p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AccountControllerIT {

    private static final String URL = "/api/v1/accounts";

    @Autowired MockMvc         mockMvc;
    @Autowired ObjectMapper    objectMapper;
    @Autowired AccountRepository accountRepository;

    private UUID testCustomerId;

    @BeforeEach
    void setUp() {
        testCustomerId = UUID.randomUUID();
    }

    /** Convenience: build a JWT post-processor with {@code sub} = customerId. */
    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtFor(UUID customerId) {
        return jwt().jwt(builder -> builder.subject(customerId.toString()));
    }

    // ────────────────────────────────────────────────────────────────────────
    // AC1 / AC2 / AC3 / AC4 — Happy path
    // ────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("AC1-4: Happy path")
    class HappyPath {

        @Test
        @DisplayName("CHECKING account → 201, 10-digit number, balance=0, status=ACTIVE, Location header")
        void openCheckingAccount_returns201WithExpectedPayload() throws Exception {
            var request = new OpenAccountRequest(AccountType.CHECKING);

            var result = mockMvc.perform(post(URL)
                            .with(jwtFor(testCustomerId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.accountNumber").value(matchesPattern("\\d{10}")))  // AC2
                    .andExpect(jsonPath("$.type").value("CHECKING"))                          // AC1
                    .andExpect(jsonPath("$.balance").value(0.00))                             // AC3
                    .andExpect(jsonPath("$.status").value("ACTIVE"))                         // AC3
                    .andExpect(jsonPath("$.createdAt").exists())                              // AC4
                    .andReturn();

            // AC2: verify persisted in DB with correct data
            String body      = result.getResponse().getContentAsString();
            UUID   accountId = UUID.fromString(objectMapper.readTree(body).get("id").asText());
            var    persisted = accountRepository.findById(accountId);
            assertThat(persisted).isPresent();
            assertThat(persisted.get().getCustomerId()).isEqualTo(testCustomerId);
            assertThat(persisted.get().getAccountNumber()).matches("\\d{10}");
        }

        @Test
        @DisplayName("SAVINGS account → 201 with type=SAVINGS")
        void openSavingsAccount_returns201() throws Exception {
            mockMvc.perform(post(URL)
                            .with(jwtFor(testCustomerId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new OpenAccountRequest(AccountType.SAVINGS))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.type").value("SAVINGS"))
                    .andExpect(jsonPath("$.balance").value(0.00))
                    .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("Location header points to /api/v1/accounts/{id}")
        void openAccount_locationHeaderContainsNewResourceId() throws Exception {
            var result = mockMvc.perform(post(URL)
                            .with(jwtFor(testCustomerId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new OpenAccountRequest(AccountType.CHECKING))))
                    .andExpect(status().isCreated())
                    .andReturn();

            String location = result.getResponse().getHeader("Location");
            String body     = result.getResponse().getContentAsString();
            String id       = objectMapper.readTree(body).get("id").asText();

            assertThat(location).endsWith("/api/v1/accounts/" + id);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // AC5 — Duplicate type enforcement
    // ────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("AC5: Duplicate type enforcement")
    class DuplicateType {

        @Test
        @DisplayName("Second CHECKING for same customer → 409 with spec message")
        void openDuplicateChecking_returns409WithSpecMessage() throws Exception {
            var request = new OpenAccountRequest(AccountType.CHECKING);

            // First open: succeeds
            mockMvc.perform(post(URL)
                            .with(jwtFor(testCustomerId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            // Second open: same type → 409
            mockMvc.perform(post(URL)
                            .with(jwtFor(testCustomerId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail").value("Account of this type already exists"));
        }

        @Test
        @DisplayName("Second SAVINGS for same customer → 409")
        void openDuplicateSavings_returns409() throws Exception {
            var request = new OpenAccountRequest(AccountType.SAVINGS);
            mockMvc.perform(post(URL).with(jwtFor(testCustomerId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            mockMvc.perform(post(URL).with(jwtFor(testCustomerId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("One CHECKING + one SAVINGS for same customer is allowed")
        void oneCheckingOneSavings_bothAllowed() throws Exception {
            mockMvc.perform(post(URL).with(jwtFor(testCustomerId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new OpenAccountRequest(AccountType.CHECKING))))
                    .andExpect(status().isCreated());

            mockMvc.perform(post(URL).with(jwtFor(testCustomerId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new OpenAccountRequest(AccountType.SAVINGS))))
                    .andExpect(status().isCreated());

            assertThat(accountRepository
                    .existsByCustomerIdAndType(testCustomerId, AccountType.CHECKING)).isTrue();
            assertThat(accountRepository
                    .existsByCustomerIdAndType(testCustomerId, AccountType.SAVINGS)).isTrue();
        }

        @Test
        @DisplayName("Different customers can each open the same account type independently")
        void differentCustomers_canOpenSameType() throws Exception {
            UUID customer2 = UUID.randomUUID();
            var request = new OpenAccountRequest(AccountType.CHECKING);

            mockMvc.perform(post(URL).with(jwtFor(testCustomerId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            mockMvc.perform(post(URL).with(jwtFor(customer2))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // AC6 — Authentication enforcement
    // ────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("AC6: Authentication enforcement")
    class AuthEnforcement {

        @Test
        @DisplayName("No JWT → 401 Unauthorized")
        void noJwt_returns401() throws Exception {
            mockMvc.perform(post(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new OpenAccountRequest(AccountType.CHECKING))))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Validation — Request body constraints
    // ────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Request validation")
    class RequestValidation {

        @Test
        @DisplayName("Null type field → 400 Bad Request")
        void nullType_returns400() throws Exception {
            mockMvc.perform(post(URL)
                            .with(jwtFor(testCustomerId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"type\": null}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Missing type field → 400 Bad Request")
        void missingType_returns400() throws Exception {
            mockMvc.perform(post(URL)
                            .with(jwtFor(testCustomerId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Invalid type value 'CREDIT' → 400 Bad Request")
        void invalidTypeString_returns400() throws Exception {
            mockMvc.perform(post(URL)
                            .with(jwtFor(testCustomerId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"type\": \"CREDIT\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Empty body → 400 Bad Request")
        void emptyBody_returns400() throws Exception {
            mockMvc.perform(post(URL)
                            .with(jwtFor(testCustomerId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(""))
                    .andExpect(status().isBadRequest());
        }
    }
}
