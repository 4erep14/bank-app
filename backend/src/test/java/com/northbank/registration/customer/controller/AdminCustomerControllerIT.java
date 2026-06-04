// Story: US-019
package com.northbank.registration.customer.controller;

import com.northbank.registration.account.controller.TestJwtHelper;
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

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminCustomerControllerIT extends IntegrationTestBase {

    private static final String BASE_URL = "/api/v1/admin/customers";

    @Autowired
    CustomerRepository customerRepository;

    private Customer admin;
    private Customer activeCustomer;
    private Customer lockedCustomer;

    @BeforeEach
    void setUp() {
        admin = customerRepository.save(customer("admin@bank.test", CustomerStatus.ACTIVE, CustomerRole.ADMIN));
        activeCustomer = customerRepository.save(customer("active@bank.test", CustomerStatus.ACTIVE, CustomerRole.CUSTOMER));
        lockedCustomer = customerRepository.save(customer("locked@bank.test", CustomerStatus.LOCKED, CustomerRole.CUSTOMER));
    }

    @Test
    @DisplayName("AC1: admin lists customers with default page size 20")
    void listCustomers_returnsPage() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .header("Authorization", "Bearer " + TestJwtHelper.generateAdminToken(admin.getId()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page.size").value(20))
                .andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.content[0].fullName").exists())
                .andExpect(jsonPath("$.content[0].phone").exists());
    }

    @Test
    @DisplayName("AC2: admin filters customers by status")
    void listCustomers_filtersByStatus() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .param("status", "LOCKED")
                        .header("Authorization", "Bearer " + TestJwtHelper.generateAdminToken(admin.getId()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].status", everyItem(is("LOCKED"))));
    }

    @Test
    @DisplayName("AC3: admin deactivates customer and stale JWT is rejected")
    void deactivateCustomer_setsInactiveAndInvalidatesJwt() throws Exception {
        String customerToken = TestJwtHelper.generateToken(activeCustomer.getId());

        mockMvc.perform(patch(BASE_URL + "/{id}/deactivate", activeCustomer.getId())
                        .header("Authorization", "Bearer " + TestJwtHelper.generateAdminToken(admin.getId()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        mockMvc.perform(get("/api/v1/profile")
                        .header("Authorization", "Bearer " + customerToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("AC4: admin unlocks locked customer")
    void unlockCustomer_setsActive() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/{id}/unlock", lockedCustomer.getId())
                        .header("Authorization", "Bearer " + TestJwtHelper.generateAdminToken(admin.getId()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("AC5: admin cannot deactivate another admin")
    void deactivateAdmin_returns403() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/{id}/deactivate", admin.getId())
                        .header("Authorization", "Bearer " + TestJwtHelper.generateAdminToken(admin.getId()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Cannot deactivate an admin account"));
    }

    @Test
    @DisplayName("AC6: non-admin cannot use admin customer endpoints")
    void nonAdmin_returns403() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .header("Authorization", "Bearer " + TestJwtHelper.generateToken(activeCustomer.getId()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    private Customer customer(String email, CustomerStatus status, CustomerRole role) {
        String name = email.substring(0, email.indexOf('@'));
        return Customer.builder()
                .firstName(name)
                .lastName("User")
                .email(email)
                .phoneNumber("+10000000001")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .passwordHash("$2a$12$dummy")
                .status(status)
                .role(role)
                .build();
    }
}
