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
import com.northbank.registration.transaction.domain.model.Transaction;
import com.northbank.registration.transaction.domain.model.TransactionType;
import com.northbank.registration.transaction.repository.TransactionRepository;
import com.northbank.registration.transaction.service.FraudEvaluationPort;
import com.northbank.registration.transaction.service.FraudEvaluationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Test
    @DisplayName("US-011: customer can list own transaction history with filters")
    void listTransactions_returnsOnlyOwnFilteredTransactions() throws Exception {
        seedTransaction(customerId, checking.getId(), savings.getId(), "20.00", TransactionStatus.COMPLETED, "Visible");
        seedTransaction(customerId, savings.getId(), checking.getId(), "15.00", TransactionStatus.BLOCKED, "Hidden by status");
        BankAccount other = seedAccount(otherCustomerId, AccountType.CHECKING, "4234567890", new BigDecimal("50.00"), AccountStatus.ACTIVE);
        seedTransaction(otherCustomerId, other.getId(), checking.getId(), "7.00", TransactionStatus.COMPLETED, "Other customer");

        mockMvc.perform(get("/api/v1/transactions")
                        .param("status", "COMPLETED")
                        .with(jwt().jwt(j -> j.subject(customerId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].description").value("Visible"))
                .andExpect(jsonPath("$.content[0].customerId").value(customerId.toString()));
    }

    @Test
    @DisplayName("US-012: customer can view own transaction detail")
    void getTransaction_ownTransaction_returnsDetail() throws Exception {
        Transaction transaction = seedTransaction(customerId, checking.getId(), savings.getId(), "44.00", TransactionStatus.COMPLETED, "Detail");

        mockMvc.perform(get("/api/v1/transactions/{id}", transaction.getId())
                        .with(jwt().jwt(j -> j.subject(customerId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transaction.getId().toString()))
                .andExpect(jsonPath("$.sourceAccountId").value(checking.getId().toString()))
                .andExpect(jsonPath("$.destinationAccountId").value(savings.getId().toString()));
    }

    @Test
    @DisplayName("US-012: customer cannot view another customer's transaction")
    void getTransaction_otherCustomerTransaction_returns404() throws Exception {
        BankAccount other = seedAccount(otherCustomerId, AccountType.CHECKING, "5234567890", new BigDecimal("50.00"), AccountStatus.ACTIVE);
        Transaction transaction = seedTransaction(otherCustomerId, other.getId(), checking.getId(), "12.00", TransactionStatus.COMPLETED, "Other");

        mockMvc.perform(get("/api/v1/transactions/{id}", transaction.getId())
                        .with(jwt().jwt(j -> j.subject(customerId.toString()))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("US-013: admin can list transactions across customers")
    void adminListTransactions_withAdminRole_returnsSystemWideTransactions() throws Exception {
        seedTransaction(customerId, checking.getId(), savings.getId(), "20.00", TransactionStatus.COMPLETED, "Customer one");
        BankAccount other = seedAccount(otherCustomerId, AccountType.CHECKING, "6234567890", new BigDecimal("50.00"), AccountStatus.ACTIVE);
        seedTransaction(otherCustomerId, other.getId(), savings.getId(), "7.00", TransactionStatus.BLOCKED, "Customer two");

        mockMvc.perform(get("/api/v1/admin/transactions")
                        .with(jwt()
                                .jwt(j -> j.subject(customerId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    @DisplayName("US-013: non-admin cannot access admin transaction overview")
    void adminListTransactions_withoutAdminRole_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/transactions")
                        .with(jwt().jwt(j -> j.subject(customerId.toString()))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("US-013: admin can view any transaction detail")
    void adminGetTransaction_withAdminRole_returnsDetail() throws Exception {
        Transaction transaction = seedTransaction(customerId, checking.getId(), savings.getId(), "88.00", TransactionStatus.COMPLETED, "Admin detail");

        mockMvc.perform(get("/api/v1/admin/transactions/{id}", transaction.getId())
                        .with(jwt()
                                .jwt(j -> j.subject(customerId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transaction.getId().toString()))
                .andExpect(jsonPath("$.customerId").value(customerId.toString()));
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

    private Transaction seedTransaction(
            UUID ownerId,
            UUID sourceId,
            UUID destinationId,
            String amount,
            TransactionStatus status,
            String description) {

        return transactionRepository.saveAndFlush(Transaction.builder()
                .customerId(ownerId)
                .type(TransactionType.TRANSFER)
                .status(status)
                .amount(new BigDecimal(amount))
                .sourceAccountId(sourceId)
                .destinationAccountId(destinationId)
                .description(description)
                .timestamp(OffsetDateTime.now())
                .build());
    }
}
