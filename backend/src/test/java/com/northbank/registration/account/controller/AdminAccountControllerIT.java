// Story: US-009
package com.northbank.registration.account.controller;

import com.northbank.registration.account.domain.model.AccountStatus;
import com.northbank.registration.account.domain.model.AccountType;
import com.northbank.registration.account.domain.model.BankAccount;
import com.northbank.registration.account.repository.AccountRepository;
import com.northbank.registration.customer.domain.model.Customer;
import com.northbank.registration.customer.domain.model.CustomerRole;
import com.northbank.registration.customer.repository.CustomerRepository;
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

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for US-009: Admin — View & Manage Customer Accounts.
 *
 * <p>Test matrix:
 * <ul>
 *   <li>AC1: GET /admin/accounts → 200, page structure, default size 20</li>
 *   <li>AC2: Response includes ownerFullName, ownerEmail</li>
 *   <li>AC3: Filters by customerId, type, status</li>
 *   <li>AC4: PATCH /deactivate → 200, status=INACTIVE; 409 if already inactive</li>
 *   <li>AC5: PATCH /activate  → 200, status=ACTIVE; 409 if not inactive</li>
 *   <li>AC6: Non-admin (CUSTOMER role) → 403 on all admin endpoints</li>
 * </ul>
 * </p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminAccountControllerIT {

    private static final String BASE_URL      = "/api/v1/admin/accounts";
    private static final String DEACTIVATE    = BASE_URL + "/{id}/deactivate";
    private static final String ACTIVATE      = BASE_URL + "/{id}/activate";

    @Autowired MockMvc          mockMvc;
    @Autowired AccountRepository  accountRepository;
    @Autowired CustomerRepository customerRepository;

    private Customer adminCustomer;
    private Customer regularCustomer;
    private BankAccount account;

    @BeforeEach
    void setUp() {
        // Admin user
        adminCustomer = customerRepository.save(Customer.builder()
                .firstName("Admin").lastName("User")
                .email("admin@bank.test")
                .phoneNumber("+10000000000")
                .dateOfBirth(LocalDate.of(1980, 1, 1))
                .passwordHash("$2a$12$dummy")
                .role(CustomerRole.ADMIN)
                .build());

        // Regular customer
        regularCustomer = customerRepository.save(Customer.builder()
                .firstName("Jane").lastName("Doe")
                .email("jane.doe@bank.test")
                .phoneNumber("+10000000001")
                .dateOfBirth(LocalDate.of(1990, 6, 15))
                .passwordHash("$2a$12$dummy")
                .role(CustomerRole.CUSTOMER)
                .build());

        // One CHECKING account owned by regular customer
        account = accountRepository.save(BankAccount.builder()
                .accountNumber("4823901754")
                .type(AccountType.CHECKING)
                .customerId(regularCustomer.getId())
                .build());
    }

    // ── AC1 + AC2: List all accounts ─────────────────────────────────────────

    @Nested
    @DisplayName("AC1+AC2: Paginated list with owner info")
    class ListAccounts {

        @Test
        @DisplayName("Admin gets 200 with page structure and ownerFullName + ownerEmail (AC2)")
        void admin_gets200WithOwnerInfo() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .header("Authorization", "Bearer " + TestJwtHelper.generateAdminToken(adminCustomer.getId()))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].accountNumber").value("4823901754"))
                    .andExpect(jsonPath("$.content[0].ownerFullName").value("Jane Doe"))
                    .andExpect(jsonPath("$.content[0].ownerEmail").value("jane.doe@bank.test"))
                    .andExpect(jsonPath("$.page.size").value(20))
                    .andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(1)));
        }

        @Test
        @DisplayName("Default page size is 20 (AC1)")
        void defaultPageSizeIs20() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .header("Authorization", "Bearer " + TestJwtHelper.generateAdminToken(adminCustomer.getId()))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page.size").value(20));
        }
    }

    // ── AC3: Filters ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("AC3: Filters")
    class Filters {

        @Test
        @DisplayName("Filter by customerId returns only that customer's accounts")
        void filterByCustomerId() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .param("customerId", regularCustomer.getId().toString())
                            .header("Authorization", "Bearer " + TestJwtHelper.generateAdminToken(adminCustomer.getId()))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].ownerEmail", everyItem(is("jane.doe@bank.test"))));
        }

        @Test
        @DisplayName("Filter by type=CHECKING returns only CHECKING accounts")
        void filterByType() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .param("type", "CHECKING")
                            .header("Authorization", "Bearer " + TestJwtHelper.generateAdminToken(adminCustomer.getId()))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].type", everyItem(is("CHECKING"))));
        }

        @Test
        @DisplayName("Filter by status=ACTIVE returns only ACTIVE accounts")
        void filterByStatus() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .param("status", "ACTIVE")
                            .header("Authorization", "Bearer " + TestJwtHelper.generateAdminToken(adminCustomer.getId()))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].status", everyItem(is("ACTIVE"))));
        }
    }

    // ── AC4: Deactivate ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("AC4: Deactivate account")
    class Deactivate {

        @Test
        @DisplayName("Admin deactivates ACTIVE account → 200, status=INACTIVE")
        void deactivate_active_returns200() throws Exception {
            mockMvc.perform(patch(DEACTIVATE, account.getId())
                            .header("Authorization", "Bearer " + TestJwtHelper.generateAdminToken(adminCustomer.getId()))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("INACTIVE"))
                    .andExpect(jsonPath("$.id").value(account.getId().toString()));
        }

        @Test
        @DisplayName("Deactivate already-inactive account → 409")
        void deactivate_alreadyInactive_returns409() throws Exception {
            account.setStatus(AccountStatus.INACTIVE);
            accountRepository.save(account);

            mockMvc.perform(patch(DEACTIVATE, account.getId())
                            .header("Authorization", "Bearer " + TestJwtHelper.generateAdminToken(adminCustomer.getId()))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Deactivate unknown account → 404")
        void deactivate_unknownId_returns404() throws Exception {
            mockMvc.perform(patch(DEACTIVATE, UUID.randomUUID())
                            .header("Authorization", "Bearer " + TestJwtHelper.generateAdminToken(adminCustomer.getId()))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }

    // ── AC5: Activate ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("AC5: Activate account")
    class Activate {

        @Test
        @DisplayName("Admin activates INACTIVE account → 200, status=ACTIVE")
        void activate_inactive_returns200() throws Exception {
            account.setStatus(AccountStatus.INACTIVE);
            accountRepository.save(account);

            mockMvc.perform(patch(ACTIVATE, account.getId())
                            .header("Authorization", "Bearer " + TestJwtHelper.generateAdminToken(adminCustomer.getId()))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("Activate already-active account → 409")
        void activate_alreadyActive_returns409() throws Exception {
            mockMvc.perform(patch(ACTIVATE, account.getId())
                            .header("Authorization", "Bearer " + TestJwtHelper.generateAdminToken(adminCustomer.getId()))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict());
        }
    }

    // ── AC6: Role enforcement ─────────────────────────────────────────────────

    @Nested
    @DisplayName("AC6: Non-admin → 403")
    class RoleEnforcement {

        @Test
        @DisplayName("CUSTOMER role on GET /admin/accounts → 403")
        void customerRole_listAccounts_returns403() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .header("Authorization", "Bearer " + TestJwtHelper.generateToken(regularCustomer.getId()))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("CUSTOMER role on PATCH /deactivate → 403")
        void customerRole_deactivate_returns403() throws Exception {
            mockMvc.perform(patch(DEACTIVATE, account.getId())
                            .header("Authorization", "Bearer " + TestJwtHelper.generateToken(regularCustomer.getId()))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("No JWT on GET /admin/accounts → 401")
        void noJwt_returns401() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }
    }
}
