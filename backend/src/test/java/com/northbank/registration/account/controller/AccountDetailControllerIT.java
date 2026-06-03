// Story: US-008
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for US-008: View Account Details.
 *
 * <p>Covers all acceptance criteria:
 * <ul>
 *   <li>AC1: GET /api/v1/accounts/{id} is accessible to authenticated owner</li>
 *   <li>AC2: Response includes id, accountNumber, type, balance, status, createdAt</li>
 *   <li>AC3: Different customer → 403</li>
 *   <li>AC4: Unknown account ID → 404</li>
 *   <li>AC5: No JWT → 401</li>
 * </ul>
 * Each test is rolled back via {@code @Transactional}.
 * </p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AccountDetailControllerIT {

    private static final String URL = "/api/v1/accounts/{id}";

    @Autowired MockMvc mockMvc;
    @Autowired AccountRepository accountRepository;

    private UUID ownerCustomerId;
    private UUID otherCustomerId;
    private BankAccount ownerAccount;

    @BeforeEach
    void setUp() {
        ownerCustomerId = UUID.randomUUID();
        otherCustomerId = UUID.randomUUID();

        ownerAccount = accountRepository.save(BankAccount.builder()
                .accountNumber("4823901754")
                .type(AccountType.CHECKING)
                .customerId(ownerCustomerId)
                .build());
    }

    // ── AC1 + AC2: Successful retrieval ──────────────────────────────────────

    @Nested
    @DisplayName("AC1+AC2: Successful retrieval")
    class Success {

        @Test
        @DisplayName("Owner gets 200 with all required fields (AC1, AC2)")
        void owner_gets200WithAllFields() throws Exception {
            mockMvc.perform(get(URL, ownerAccount.getId())
                            .header("Authorization", "Bearer " + TestJwtHelper.generateToken(ownerCustomerId))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(ownerAccount.getId().toString()))
                    .andExpect(jsonPath("$.accountNumber").value("4823901754"))
                    .andExpect(jsonPath("$.type").value("CHECKING"))
                    .andExpect(jsonPath("$.balance").value(0.00))
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.createdAt").exists());
        }

        @Test
        @DisplayName("Balance is serialised with two decimal places (AC2)")
        void balance_hasTwoDecimalPlaces() throws Exception {
            ownerAccount.setBalance(new BigDecimal("1500.5"));
            accountRepository.save(ownerAccount);

            mockMvc.perform(get(URL, ownerAccount.getId())
                            .header("Authorization", "Bearer " + TestJwtHelper.generateToken(ownerCustomerId))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.balance").value(1500.50));
        }

        @Test
        @DisplayName("SAVINGS account is also retrievable (AC1)")
        void savingsAccount_isRetrievable() throws Exception {
            BankAccount savings = accountRepository.save(BankAccount.builder()
                    .accountNumber("9912345678")
                    .type(AccountType.SAVINGS)
                    .customerId(ownerCustomerId)
                    .build());

            mockMvc.perform(get(URL, savings.getId())
                            .header("Authorization", "Bearer " + TestJwtHelper.generateToken(ownerCustomerId))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type").value("SAVINGS"));
        }
    }

    // ── AC3: Ownership enforcement ────────────────────────────────────────────

    @Nested
    @DisplayName("AC3: Ownership enforcement")
    class Ownership {

        @Test
        @DisplayName("Different customer → 403 Forbidden")
        void differentCustomer_gets403() throws Exception {
            mockMvc.perform(get(URL, ownerAccount.getId())
                            .header("Authorization", "Bearer " + TestJwtHelper.generateToken(otherCustomerId))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail").value("Access to this account is denied"));
        }

        @Test
        @DisplayName("403 response must not leak any account fields (AC3)")
        void forbidden_doesNotLeakAccountData() throws Exception {
            mockMvc.perform(get(URL, ownerAccount.getId())
                            .header("Authorization", "Bearer " + TestJwtHelper.generateToken(otherCustomerId))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.accountNumber").doesNotExist())
                    .andExpect(jsonPath("$.balance").doesNotExist())
                    .andExpect(jsonPath("$.type").doesNotExist());
        }
    }

    // ── AC4: Not found ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("AC4: Account not found")
    class NotFound {

        @Test
        @DisplayName("Random UUID → 404 Not Found")
        void randomId_gets404() throws Exception {
            mockMvc.perform(get(URL, UUID.randomUUID())
                            .header("Authorization", "Bearer " + TestJwtHelper.generateToken(ownerCustomerId))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value("Account not found"));
        }
    }

    // ── AC5: Auth enforcement ─────────────────────────────────────────────────

    @Nested
    @DisplayName("AC5: Authentication enforcement")
    class Auth {

        @Test
        @DisplayName("No JWT → 401 Unauthorized")
        void noJwt_gets401() throws Exception {
            mockMvc.perform(get(URL, ownerAccount.getId())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }
    }
}
