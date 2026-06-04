// Story: US-020
package com.northbank.registration.audit.controller;

import com.northbank.registration.account.controller.TestJwtHelper;
import com.northbank.registration.audit.domain.model.AuditActionType;
import com.northbank.registration.audit.service.AuditLogService;
import com.northbank.registration.config.IntegrationTestBase;
import com.northbank.registration.customer.domain.model.Customer;
import com.northbank.registration.customer.domain.model.CustomerRole;
import com.northbank.registration.customer.domain.model.CustomerStatus;
import com.northbank.registration.customer.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminAuditLogControllerIT extends IntegrationTestBase {

    private static final String BASE_URL = "/api/v1/admin/audit-logs";

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    AuditLogService auditLogService;

    private Customer admin;
    private Customer customer;

    @BeforeEach
    void setUp() {
        admin = customerRepository.save(customer("audit.admin@bank.test", CustomerRole.ADMIN));
        customer = customerRepository.save(customer("audit.customer@bank.test", CustomerRole.CUSTOMER));

        auditLogService.record(AuditActionType.LOGIN_SUCCESS, customer.getId(), "CUSTOMER", "CUSTOMER", customer.getId());
        auditLogService.record(AuditActionType.CUSTOMER_UNLOCKED, admin.getId(), "ADMIN", "CUSTOMER", customer.getId());
    }

    @Test
    @DisplayName("AC1+AC2: admin lists audit logs with default page size 50 and required fields")
    void listAuditLogs_returnsPage() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .header("Authorization", "Bearer " + TestJwtHelper.generateAdminToken(admin.getId()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size").value(50))
                .andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.content[0].id").exists())
                .andExpect(jsonPath("$.content[0].actorId").exists())
                .andExpect(jsonPath("$.content[0].actorRole").exists())
                .andExpect(jsonPath("$.content[0].actionType").exists())
                .andExpect(jsonPath("$.content[0].targetEntityType").exists())
                .andExpect(jsonPath("$.content[0].targetEntityId").exists())
                .andExpect(jsonPath("$.content[0].timestamp").exists());
    }

    @Test
    @DisplayName("AC4: admin filters audit logs by actorId and actionType")
    void listAuditLogs_filters() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .param("actorId", customer.getId().toString())
                        .param("actionType", "LOGIN_SUCCESS")
                        .header("Authorization", "Bearer " + TestJwtHelper.generateAdminToken(admin.getId()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].actorId", everyItem(is(customer.getId().toString()))))
                .andExpect(jsonPath("$.content[*].actionType", everyItem(is("LOGIN_SUCCESS"))));
    }

    @Test
    @DisplayName("AC5: audit log mutation endpoints return 405")
    void mutationEndpoints_return405() throws Exception {
        String token = TestJwtHelper.generateAdminToken(admin.getId());

        mockMvc.perform(put(BASE_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(patch(BASE_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(delete(BASE_URL)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("AC6: non-admin cannot read audit logs")
    void nonAdmin_returns403() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .header("Authorization", "Bearer " + TestJwtHelper.generateToken(customer.getId()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    private Customer customer(String email, CustomerRole role) {
        return Customer.builder()
                .id(UUID.randomUUID())
                .firstName("Audit")
                .lastName(role.name())
                .email(email)
                .phoneNumber("+10000000002")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .passwordHash("$2a$12$dummy")
                .status(CustomerStatus.ACTIVE)
                .role(role)
                .build();
    }
}
