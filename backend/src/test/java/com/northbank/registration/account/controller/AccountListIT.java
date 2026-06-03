// Story: US-007
package com.northbank.registration.account.controller;

import com.northbank.registration.account.domain.model.AccountType;
import com.northbank.registration.account.domain.model.BankAccount;
import com.northbank.registration.account.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for US-007: View Account List &amp; Balances.
 *
 * <p>Uses Spring Security Test's {@code jwt()} post-processor to inject
 * a mock JWT without starting a real auth server.</p>
 *
 * <p>Each test is rolled back via {@code @Transactional}.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AccountListIT {

    private static final String URL = "/api/v1/accounts";

    @Autowired MockMvc          mockMvc;
    @Autowired AccountRepository accountRepository;

    private UUID customerId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
    }

    // ── AC4: Empty list ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("AC4: Customer with no accounts")
    class EmptyList {

        @Test
        @DisplayName("GET /api/v1/accounts → 200 with empty array []")
        void noAccounts_returns200EmptyArray() throws Exception {
            mockMvc.perform(get(URL)
                            .with(jwt().jwt(j -> j.subject(customerId.toString())))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    // ── AC1 / AC2 / AC3 / AC5 ────────────────────────────────────────────────

    @Nested
    @DisplayName("AC1 / AC2 / AC3: Customer with accounts")
    class WithAccounts {

        @Test
        @DisplayName("AC1: GET /api/v1/accounts → 200 OK")
        void get_returns200() throws Exception {
            seedAccount(customerId, AccountType.CHECKING, "1234567890", new BigDecimal("100.00"));

            mockMvc.perform(get(URL)
                            .with(jwt().jwt(j -> j.subject(customerId.toString())))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("AC2: Each item contains accountNumber, type, balance, status")
        void listItem_containsRequiredFields() throws Exception {
            seedAccount(customerId, AccountType.CHECKING, "1234567890", new BigDecimal("1250.00"));

            mockMvc.perform(get(URL)
                            .with(jwt().jwt(j -> j.subject(customerId.toString())))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].accountNumber").value("1234567890"))
                    .andExpect(jsonPath("$[0].type").value("CHECKING"))
                    .andExpect(jsonPath("$[0].balance").value(1250.00))
                    .andExpect(jsonPath("$[0].status").value("ACTIVE"));
        }

        @Test
        @DisplayName("AC3: Only returns accounts belonging to the authenticated customer")
        void ac3_returnsOnlyOwnAccounts() throws Exception {
            // Seed one account for our customer, one for a different customer
            seedAccount(customerId,       AccountType.CHECKING, "1111111111", new BigDecimal("500.00"));
            seedAccount(UUID.randomUUID(), AccountType.SAVINGS,  "9999999999", new BigDecimal("999.99"));

            mockMvc.perform(get(URL)
                            .with(jwt().jwt(j -> j.subject(customerId.toString())))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].accountNumber").value("1111111111"));
        }

        @Test
        @DisplayName("AC3: Different customers see only their own accounts")
        void ac3_differentCustomersSeeOnlyTheirOwn() throws Exception {
            UUID otherCustomer = UUID.randomUUID();
            seedAccount(customerId,  AccountType.CHECKING, "1111111111", new BigDecimal("100.00"));
            seedAccount(otherCustomer, AccountType.SAVINGS, "2222222222", new BigDecimal("200.00"));

            // Our customer sees only their account
            mockMvc.perform(get(URL)
                            .with(jwt().jwt(j -> j.subject(customerId.toString())))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].accountNumber").value("1111111111"));

            // Other customer sees only their account
            mockMvc.perform(get(URL)
                            .with(jwt().jwt(j -> j.subject(otherCustomer.toString())))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].accountNumber").value("2222222222"));
        }

        @Test
        @DisplayName("AC5: Balance formatted to exactly 2 decimal places")
        void ac5_balanceTwoDecimalPlaces() throws Exception {
            seedAccount(customerId, AccountType.SAVINGS, "5555000011", new BigDecimal("0.00"));

            mockMvc.perform(get(URL)
                            .with(jwt().jwt(j -> j.subject(customerId.toString())))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].balance").value(0.00));
        }

        @Test
        @DisplayName("Both CHECKING and SAVINGS returned for same customer")
        void bothTypes_returnedForSameCustomer() throws Exception {
            seedAccount(customerId, AccountType.CHECKING, "1111111111", new BigDecimal("100.00"));
            seedAccount(customerId, AccountType.SAVINGS,  "2222222222", new BigDecimal("200.00"));

            mockMvc.perform(get(URL)
                            .with(jwt().jwt(j -> j.subject(customerId.toString())))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[*].type", containsInAnyOrder("CHECKING", "SAVINGS")));
        }

        @Test
        @DisplayName("Results ordered newest-first")
        void results_orderedNewestFirst() throws Exception {
            // Save CHECKING first, SAVINGS second — list should show SAVINGS at index 0
            seedAccount(customerId, AccountType.CHECKING, "1111111111", new BigDecimal("100.00"));
            seedAccount(customerId, AccountType.SAVINGS,  "2222222222", new BigDecimal("200.00"));

            mockMvc.perform(get(URL)
                            .with(jwt().jwt(j -> j.subject(customerId.toString())))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].type").value("SAVINGS"));
        }
    }

    // ── AC6: Auth enforcement ─────────────────────────────────────────────────

    @Nested
    @DisplayName("AC6: Auth enforcement")
    class AuthEnforcement {

        @Test
        @DisplayName("No JWT → 401 Unauthorized")
        void noJwt_returns401() throws Exception {
            mockMvc.perform(get(URL).accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void seedAccount(UUID ownerId, AccountType type, String number, BigDecimal balance) {
        accountRepository.save(BankAccount.builder()
                .customerId(ownerId)
                .type(type)
                .accountNumber(number)
                .balance(balance)
                .build());
    }
}
