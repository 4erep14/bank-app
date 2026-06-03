// Story: US-010
package com.northbank.registration.transaction.controller;

import com.northbank.registration.account.domain.model.AccountStatus;
import com.northbank.registration.account.domain.model.AccountType;
import com.northbank.registration.account.domain.model.BankAccount;
import com.northbank.registration.account.repository.AccountRepository;
import com.northbank.registration.config.IntegrationTestBase;
import com.northbank.registration.customer.domain.model.Customer;
import com.northbank.registration.customer.domain.model.CustomerStatus;
import com.northbank.registration.customer.repository.CustomerRepository;
import com.northbank.registration.transaction.domain.model.TransactionStatus;
import com.northbank.registration.transaction.repository.TransactionRepository;
import com.northbank.registration.transaction.service.FraudEvaluationPort;
import com.northbank.registration.transaction.service.FraudEvaluationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransactionControllerIT extends IntegrationTestBase {

    private static final String URL = "/api/v1/transactions/transfer";

    @Autowired CustomerRepository customerRepository;
    @Autowired AccountRepository accountRepository;
    @Autowired TransactionRepository transactionRepository;

    @MockBean FraudEvaluationPort fraudEvaluationPort;

    private UUID customerId;
    private UUID otherCustomerId;
    private BankAccount checking;
    private BankAccount savings;

    @BeforeEach
    void setUp() {
        customerId = seedCustomer("primary-us010@example.com");
        otherCustomerId = seedCustomer("other-us010@example.com");
        checking = seedAccount(customerId, AccountType.CHECKING, "1234567890", new BigDecimal("500.00"), AccountStatus.ACTIVE);
        savings = seedAccount(customerId, AccountType.SAVINGS, "2234567890", new BigDecimal("100.00"), AccountStatus.ACTIVE);
        when(fraudEvaluationPort.evaluate(any())).thenReturn(FraudEvaluationResult.allowed());
    }

    @Test
    @DisplayName("AC1/6/7/10: valid transfer returns 201, moves balances, creates COMPLETED transaction")
    void transfer_validRequest_completesAtomically() throws Exception {
        var result = mockMvc.perform(post(URL)
                        .with(jwt().jwt(j -> j.subject(customerId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(checking.getId(), savings.getId(), "125.50", "Move to savings")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").exists())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andReturn();

        UUID transactionId = UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString())
                .get("transactionId").asText());

        var updatedChecking = accountRepository.findById(checking.getId()).orElseThrow();
        var updatedSavings = accountRepository.findById(savings.getId()).orElseThrow();
        var transaction = transactionRepository.findById(transactionId).orElseThrow();

        assertThat(updatedChecking.getBalance()).isEqualByComparingTo("374.50");
        assertThat(updatedSavings.getBalance()).isEqualByComparingTo("225.50");
        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(transaction.getDescription()).isEqualTo("Move to savings");
    }

    @Test
    @DisplayName("AC3: same source and destination returns 400 with exact message")
    void transfer_sameAccount_returns400() throws Exception {
        mockMvc.perform(post(URL)
                        .with(jwt().jwt(j -> j.subject(customerId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(checking.getId(), checking.getId(), "10.00", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Source and destination accounts must differ"));
    }

    @Test
    @DisplayName("AC2: destination owned by another customer returns 403")
    void transfer_otherCustomerAccount_returns403() throws Exception {
        BankAccount other = seedAccount(otherCustomerId, AccountType.CHECKING, "3234567890", new BigDecimal("50.00"), AccountStatus.ACTIVE);

        mockMvc.perform(post(URL)
                        .with(jwt().jwt(j -> j.subject(customerId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(checking.getId(), other.getId(), "10.00", null)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("AC4: insufficient funds returns 422 with exact message and does not move funds")
    void transfer_insufficientFunds_returns422() throws Exception {
        mockMvc.perform(post(URL)
                        .with(jwt().jwt(j -> j.subject(customerId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(checking.getId(), savings.getId(), "999.99", null)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("Insufficient funds"));

        assertThat(accountRepository.findById(checking.getId()).orElseThrow().getBalance()).isEqualByComparingTo("500.00");
        assertThat(accountRepository.findById(savings.getId()).orElseThrow().getBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("AC5: amount above limit returns 400 with field-level errors")
    void transfer_amountAboveLimit_returns400() throws Exception {
        mockMvc.perform(post(URL)
                        .with(jwt().jwt(j -> j.subject(customerId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(checking.getId(), savings.getId(), "10000.01", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("AC9: non-active account returns 422 with exact message")
    void transfer_inactiveAccount_returns422() throws Exception {
        savings.setStatus(AccountStatus.FROZEN);
        accountRepository.saveAndFlush(savings);

        mockMvc.perform(post(URL)
                        .with(jwt().jwt(j -> j.subject(customerId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(checking.getId(), savings.getId(), "25.00", null)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("Account is inactive"));
    }

    @Test
    @DisplayName("AC8: fraud-blocked transfer creates BLOCKED transaction and does not move funds")
    void transfer_fraudBlocked_returnsBlockedWithoutMovingFunds() throws Exception {
        when(fraudEvaluationPort.evaluate(any())).thenReturn(FraudEvaluationResult.blockedResult());

        mockMvc.perform(post(URL)
                        .with(jwt().jwt(j -> j.subject(customerId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(checking.getId(), savings.getId(), "25.00", null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("BLOCKED"));

        assertThat(accountRepository.findById(checking.getId()).orElseThrow().getBalance()).isEqualByComparingTo("500.00");
        assertThat(accountRepository.findById(savings.getId()).orElseThrow().getBalance()).isEqualByComparingTo("100.00");
        assertThat(transactionRepository.findAll()).hasSize(1);
        assertThat(transactionRepository.findAll().getFirst().getStatus()).isEqualTo(TransactionStatus.BLOCKED);
    }

    @Test
    @DisplayName("AC11: unauthenticated request returns 401")
    void transfer_noJwt_returns401() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(checking.getId(), savings.getId(), "10.00", null)))
                .andExpect(status().isUnauthorized());
    }

    private String json(UUID sourceId, UUID destinationId, String amount, String description) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "sourceAccountId", sourceId,
                "destinationAccountId", destinationId,
                "amount", amount,
                "description", description == null ? "" : description
        ));
    }

    private UUID seedCustomer(String email) {
        return customerRepository.save(Customer.builder()
                .firstName("Test")
                .lastName("Customer")
                .email(email)
                .phoneNumber("+15555550123")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .passwordHash("$2a$12$abcdefghijklmnopqrstuuJ4m7msmknv3ztTKy8NTkI11tE7AFs6G")
                .status(CustomerStatus.ACTIVE)
                .build()).getId();
    }

    private BankAccount seedAccount(UUID ownerId, AccountType type, String number, BigDecimal balance, AccountStatus status) {
        return accountRepository.saveAndFlush(BankAccount.builder()
                .customerId(ownerId)
                .type(type)
                .accountNumber(number)
                .balance(balance)
                .status(status)
                .build());
    }
}
