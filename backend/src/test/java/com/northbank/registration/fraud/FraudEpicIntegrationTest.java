// Story: US-014 / US-015 / US-016 / US-017 / US-018
package com.northbank.registration.fraud;

import com.northbank.registration.account.domain.model.AccountStatus;
import com.northbank.registration.account.domain.model.AccountType;
import com.northbank.registration.account.domain.model.BankAccount;
import com.northbank.registration.account.repository.AccountRepository;
import com.northbank.registration.config.IntegrationTestBase;
import com.northbank.registration.customer.domain.model.Customer;
import com.northbank.registration.customer.domain.model.CustomerStatus;
import com.northbank.registration.customer.repository.CustomerRepository;
import com.northbank.registration.fraud.domain.model.FraudAlertReviewStatus;
import com.northbank.registration.fraud.domain.model.FraudRule;
import com.northbank.registration.fraud.domain.model.FraudRuleConditionType;
import com.northbank.registration.fraud.domain.model.FraudRuleStatus;
import com.northbank.registration.fraud.repository.FraudAlertRepository;
import com.northbank.registration.fraud.repository.FraudRuleRepository;
import com.northbank.registration.notification.repository.NotificationRepository;
import com.northbank.registration.transaction.domain.model.TransactionStatus;
import com.northbank.registration.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FraudEpicIntegrationTest extends IntegrationTestBase {

    @Autowired CustomerRepository customerRepository;
    @Autowired AccountRepository accountRepository;
    @Autowired FraudRuleRepository fraudRuleRepository;
    @Autowired FraudAlertRepository fraudAlertRepository;
    @Autowired NotificationRepository notificationRepository;
    @Autowired TransactionRepository transactionRepository;

    private UUID customerId;
    private BankAccount checking;
    private BankAccount savings;

    @BeforeEach
    void setUp() {
        customerId = seedCustomer("fraud-epic@example.com");
        checking = seedAccount(customerId, AccountType.CHECKING, "7012345678", new BigDecimal("500.00"));
        savings = seedAccount(customerId, AccountType.SAVINGS, "8012345678", new BigDecimal("25.00"));
    }

    @Test
    @DisplayName("US-014: fraud analyst can create/list/update rules and duplicate names return 409")
    void fraudRuleManagement_enforcesAnalystRoleAndUniqueNames() throws Exception {
        mockMvc.perform(post("/api/v1/fraud/rules")
                        .with(fraudAnalystJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ruleJson("High amount", "AMOUNT_EXCEEDS", "100.00", true)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("High amount"));

        mockMvc.perform(post("/api/v1/fraud/rules")
                        .with(fraudAnalystJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ruleJson("High amount", "AMOUNT_EXCEEDS", "200.00", true)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Rule name already exists"));

        UUID ruleId = fraudRuleRepository.findAll().getFirst().getId();
        mockMvc.perform(patch("/api/v1/fraud/rules/{id}", ruleId)
                        .with(fraudAnalystJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("active", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/v1/fraud/rules")
                        .with(jwt().jwt(j -> j.subject(customerId.toString()))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("US-014: deleting the last active rule returns 409")
    void deleteLastActiveRule_returns409() throws Exception {
        FraudRule rule = seedRule("Last active", FraudRuleConditionType.AMOUNT_EXCEEDS, "100.00", true);

        mockMvc.perform(delete("/api/v1/fraud/rules/{id}", rule.getId())
                        .with(fraudAnalystJwt()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("At least one active rule must remain"));
    }

    @Test
    @DisplayName("US-015/016: matching rule blocks transfer and creates alert plus notification")
    void transferMatchingRule_blocksAndCreatesAlertAndNotification() throws Exception {
        seedRule("High amount", FraudRuleConditionType.AMOUNT_EXCEEDS, "100.00", true);

        mockMvc.perform(post("/api/v1/transactions/transfer")
                        .with(jwt().jwt(j -> j.subject(customerId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson("125.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("BLOCKED"));

        assertThat(accountRepository.findById(checking.getId()).orElseThrow().getBalance()).isEqualByComparingTo("500.00");
        assertThat(accountRepository.findById(savings.getId()).orElseThrow().getBalance()).isEqualByComparingTo("25.00");
        assertThat(fraudAlertRepository.findAll()).hasSize(1);
        assertThat(notificationRepository.findAll()).hasSize(1);
        assertThat(notificationRepository.findAll().getFirst().getTriggeredRuleName()).isEqualTo("High amount");

        mockMvc.perform(get("/api/v1/notifications")
                        .with(jwt().jwt(j -> j.subject(customerId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].triggeredRuleName").value("High amount"));
    }

    @Test
    @DisplayName("US-017/018: analyst can review and approve a blocked transaction")
    void approveBlockedTransaction_movesFundsAndMarksReviewed() throws Exception {
        UUID alertId = createBlockedTransfer();

        mockMvc.perform(post("/api/v1/fraud/alerts/{alertId}/approve", alertId)
                        .with(fraudAnalystJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.reviewStatus").value("REVIEWED"))
                .andExpect(jsonPath("$.transaction.status").value("COMPLETED"));

        assertThat(accountRepository.findById(checking.getId()).orElseThrow().getBalance()).isEqualByComparingTo("375.00");
        assertThat(accountRepository.findById(savings.getId()).orElseThrow().getBalance()).isEqualByComparingTo("150.00");
        assertThat(fraudAlertRepository.findById(alertId).orElseThrow().getReviewStatus()).isEqualTo(FraudAlertReviewStatus.REVIEWED);

        mockMvc.perform(post("/api/v1/fraud/alerts/{alertId}/reject", alertId)
                        .with(fraudAnalystJwt()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Transaction is already resolved"));
    }

    @Test
    @DisplayName("US-018: analyst can reject a blocked transaction without moving funds")
    void rejectBlockedTransaction_setsRejectedWithoutMovingFunds() throws Exception {
        UUID alertId = createBlockedTransfer();

        mockMvc.perform(post("/api/v1/fraud/alerts/{alertId}/reject", alertId)
                        .with(fraudAnalystJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transaction.status").value("REJECTED"));

        assertThat(accountRepository.findById(checking.getId()).orElseThrow().getBalance()).isEqualByComparingTo("500.00");
        assertThat(accountRepository.findById(savings.getId()).orElseThrow().getBalance()).isEqualByComparingTo("25.00");
        assertThat(transactionRepository.findAll().getFirst().getStatus()).isEqualTo(TransactionStatus.REJECTED);
    }

    private UUID createBlockedTransfer() throws Exception {
        seedRule("High amount " + UUID.randomUUID(), FraudRuleConditionType.AMOUNT_EXCEEDS, "100.00", true);
        mockMvc.perform(post("/api/v1/transactions/transfer")
                        .with(jwt().jwt(j -> j.subject(customerId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson("125.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("BLOCKED"));
        return fraudAlertRepository.findAll().getFirst().getId();
    }

    private String ruleJson(String name, String type, String threshold, boolean active) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "name", name,
                "conditionType", type,
                "thresholdValue", threshold,
                "active", active
        ));
    }

    private String transferJson(String amount) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "sourceAccountId", checking.getId(),
                "destinationAccountId", savings.getId(),
                "amount", amount,
                "description", "Fraud test"
        ));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor fraudAnalystJwt() {
        return jwt()
                .jwt(j -> j.subject(customerId.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_FRAUD_ANALYST"));
    }

    private FraudRule seedRule(String name, FraudRuleConditionType type, String threshold, boolean active) {
        return fraudRuleRepository.saveAndFlush(FraudRule.builder()
                .name(name)
                .conditionType(type)
                .thresholdValue(threshold)
                .active(active)
                .status(FraudRuleStatus.ACTIVE)
                .build());
    }

    private UUID seedCustomer(String email) {
        return customerRepository.save(Customer.builder()
                .firstName("Fraud")
                .lastName("Customer")
                .email(email)
                .phoneNumber("+15555550124")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .passwordHash("$2a$12$abcdefghijklmnopqrstuuJ4m7msmknv3ztTKy8NTkI11tE7AFs6G")
                .status(CustomerStatus.ACTIVE)
                .build()).getId();
    }

    private BankAccount seedAccount(UUID ownerId, AccountType type, String number, BigDecimal balance) {
        return accountRepository.saveAndFlush(BankAccount.builder()
                .customerId(ownerId)
                .type(type)
                .accountNumber(number)
                .balance(balance)
                .status(AccountStatus.ACTIVE)
                .build());
    }
}
