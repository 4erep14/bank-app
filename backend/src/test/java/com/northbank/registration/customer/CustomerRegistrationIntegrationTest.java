// Story: US-001
package com.northbank.registration.customer;

import com.northbank.registration.config.IntegrationTestBase;
import com.northbank.registration.customer.domain.model.Customer;
import com.northbank.registration.customer.domain.model.CustomerStatus;
import com.northbank.registration.customer.repository.CustomerRepository;
import com.northbank.registration.fixtures.CustomerFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for US-001: Customer Self-Registration.
 *
 * <p>Acceptance Criteria covered:</p>
 * <ul>
 *   <li>AC1 — Customer can submit a registration form with all required fields</li>
 *   <li>AC2 — Duplicate email returns 409 Conflict</li>
 *   <li>AC3 — Password complexity rules enforced; violations return 400 with field errors</li>
 *   <li>AC4 — Phone number must be E.164; invalid format returns 400 with field error</li>
 *   <li>AC5 — Persisted customer has status {@code PENDING_VERIFICATION}</li>
 *   <li>AC6 — Successful registration returns 201 with the new customer {@code id}</li>
 * </ul>
 *
 * <p>Uses a real PostgreSQL container (via {@link IntegrationTestBase}).
 * Each test is wrapped in a transaction rolled back after completion — no
 * manual teardown required.</p>
 */
@DisplayName("US-001 — Customer Self-Registration Integration Tests")
class CustomerRegistrationIntegrationTest extends IntegrationTestBase {

    private static final String ENDPOINT = "/api/v1/customers";

    @Autowired
    private CustomerRepository customerRepository;

    @BeforeEach
    void setUp() {
        customerRepository.deleteAll();
    }

    // =========================================================================
    // AC1 + AC5 + AC6 — Happy path: valid registration
    // =========================================================================

    @Test
    @DisplayName("AC1/AC5/AC6 — POST /api/v1/customers with valid data returns 201, persists customer with PENDING_VERIFICATION status, and returns the new id")
    void ac1_ac5_ac6_validRegistration_returns201WithIdAndPendingStatus() throws Exception {

        String responseBody = mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CustomerFixtures.VALID_REGISTRATION_JSON))
                .andExpect(status().isCreated())
                // AC6 — id present in response body
                .andExpect(jsonPath("$.id").isNotEmpty())
                // Location header present and contains the id
                .andExpect(header().string("Location", containsString("/api/v1/customers/")))
                // password must NEVER appear in the response
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract the returned id and verify the DB record
        UUID returnedId = objectMapper.readTree(responseBody)
                .get("id")
                .traverse(objectMapper)
                .readValueAs(UUID.class);

        // AC5 — customer persisted with PENDING_VERIFICATION
        Optional<Customer> saved = customerRepository.findById(returnedId);
        assertThat(saved).isPresent();
        Customer customer = saved.get();
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.PENDING_VERIFICATION);

        // AC1 — all fields persisted correctly
        assertThat(customer.getFirstName()).isEqualTo("Ada");
        assertThat(customer.getLastName()).isEqualTo("Lovelace");
        assertThat(customer.getEmail()).isEqualTo("ada.lovelace@example.com");
        assertThat(customer.getPhoneNumber()).isEqualTo("+14155552671");
        assertThat(customer.getDateOfBirth().toString()).isEqualTo("1990-12-10");

        // password_hash present, plaintext NOT stored
        assertThat(customer.getPasswordHash()).isNotBlank();
        assertThat(customer.getPasswordHash()).doesNotContain("Str0ng!Pass");
        assertThat(customer.getPasswordHash()).startsWith("$2a$"); // BCrypt prefix
    }

    // =========================================================================
    // AC1 — Email is normalised to lower-case before persistence
    // =========================================================================

    @Test
    @DisplayName("AC1 — Email is stored lower-cased regardless of input case")
    void ac1_emailIsNormalisedToLowerCase() throws Exception {
        String mixedCaseEmail = """
                {
                  "firstName":   "Ada",
                  "lastName":    "Lovelace",
                  "email":       "ADA.LOVELACE@EXAMPLE.COM",
                  "phoneNumber": "+14155552671",
                  "dateOfBirth": "1990-12-10",
                  "password":    "Str0ng!Pass"
                }
                """;

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mixedCaseEmail))
                .andExpect(status().isCreated());

        assertThat(customerRepository.count()).isEqualTo(1);
        Customer saved = customerRepository.findAll().get(0);
        assertThat(saved.getEmail()).isEqualTo("ada.lovelace@example.com");
    }

    // =========================================================================
    // AC2 — Duplicate email → 409 Conflict
    // =========================================================================

    @Test
    @DisplayName("AC2 — POST /api/v1/customers with already-registered email returns 409 Conflict")
    void ac2_duplicateEmail_returns409Conflict() throws Exception {
        // Pre-persist a customer with the same email
        CustomerFixtures.persistCustomer(customerRepository, CustomerFixtures.VALID_EMAIL);

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CustomerFixtures.VALID_REGISTRATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Email Already Registered"))
                .andExpect(jsonPath("$.detail").value("Email already registered"));

        // Only one customer in DB (the pre-existing one)
        assertThat(customerRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("AC2 — Duplicate-email check is case-insensitive (upper-case input matches lower-case stored)")
    void ac2_duplicateEmailCaseInsensitive_returns409() throws Exception {
        CustomerFixtures.persistCustomer(customerRepository, CustomerFixtures.VALID_EMAIL);

        String upperCaseEmailRequest = """
                {
                  "firstName":   "Charles",
                  "lastName":    "Babbage",
                  "email":       "ADA.LOVELACE@EXAMPLE.COM",
                  "phoneNumber": "+447700900123",
                  "dateOfBirth": "1985-05-20",
                  "password":    "Str0ng!Pass"
                }
                """;

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(upperCaseEmailRequest))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Email already registered"));
    }

    // =========================================================================
    // AC3 — Password complexity validation → 400
    // =========================================================================

    @Test
    @DisplayName("AC3 — Password missing uppercase returns 400 with field-level error for 'password'")
    void ac3_passwordMissingUppercase_returns400() throws Exception {
        String body = """
                {
                  "firstName":   "Ada",
                  "lastName":    "Lovelace",
                  "email":       "test@example.com",
                  "phoneNumber": "+14155552671",
                  "dateOfBirth": "1990-12-10",
                  "password":    "str0ng!pass"
                }
                """;

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[?(@.field=='password')]").isNotEmpty())
                .andExpect(jsonPath("$.errors[?(@.field=='password')].message",
                        hasItem(containsString("uppercase"))));

        assertThat(customerRepository.count()).isZero();
    }

    @Test
    @DisplayName("AC3 — Password too short (< 8 chars) returns 400 with field-level error")
    void ac3_passwordTooShort_returns400() throws Exception {
        String body = """
                {
                  "firstName":   "Ada",
                  "lastName":    "Lovelace",
                  "email":       "short@example.com",
                  "phoneNumber": "+14155552671",
                  "dateOfBirth": "1990-12-10",
                  "password":    "S1!a"
                }
                """;

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field=='password')]").isNotEmpty())
                .andExpect(jsonPath("$.errors[?(@.field=='password')].message",
                        hasItem(containsString("8 characters"))));
    }

    @Test
    @DisplayName("AC3 — Password missing digit and special char — both violations listed in error message")
    void ac3_passwordMissingMultipleRequirements_allViolationsReported() throws Exception {
        String body = """
                {
                  "firstName":   "Ada",
                  "lastName":    "Lovelace",
                  "email":       "multi@example.com",
                  "phoneNumber": "+14155552671",
                  "dateOfBirth": "1990-12-10",
                  "password":    "StrongPassword"
                }
                """;

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field=='password')].message",
                        hasItem(allOf(containsString("digit"), containsString("special character")))));
    }

    @Test
    @DisplayName("AC3 — Blank password returns 400")
    void ac3_blankPassword_returns400() throws Exception {
        String body = """
                {
                  "firstName":   "Ada",
                  "lastName":    "Lovelace",
                  "email":       "blank@example.com",
                  "phoneNumber": "+14155552671",
                  "dateOfBirth": "1990-12-10",
                  "password":    ""
                }
                """;

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field=='password')]").isNotEmpty());
    }

    // =========================================================================
    // AC4 — E.164 phone number validation → 400
    // =========================================================================

    @Test
    @DisplayName("AC4 — Phone number without leading '+' returns 400 with field error for 'phoneNumber'")
    void ac4_phoneNumberWithoutPlus_returns400() throws Exception {
        String body = """
                {
                  "firstName":   "Ada",
                  "lastName":    "Lovelace",
                  "email":       "phone@example.com",
                  "phoneNumber": "14155552671",
                  "dateOfBirth": "1990-12-10",
                  "password":    "Str0ng!Pass"
                }
                """;

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field=='phoneNumber')]").isNotEmpty())
                .andExpect(jsonPath("$.errors[?(@.field=='phoneNumber')].message",
                        hasItem(containsString("E.164"))));

        assertThat(customerRepository.count()).isZero();
    }

    @Test
    @DisplayName("AC4 — Alphabetic phone number returns 400")
    void ac4_alphabeticPhoneNumber_returns400() throws Exception {
        String body = """
                {
                  "firstName":   "Ada",
                  "lastName":    "Lovelace",
                  "email":       "abc@example.com",
                  "phoneNumber": "+ABCDEFGH",
                  "dateOfBirth": "1990-12-10",
                  "password":    "Str0ng!Pass"
                }
                """;

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field=='phoneNumber')]").isNotEmpty());
    }

    @Test
    @DisplayName("AC4 — Phone number exceeding 15 digits (E.164 max) returns 400")
    void ac4_phoneTooLong_returns400() throws Exception {
        String body = """
                {
                  "firstName":   "Ada",
                  "lastName":    "Lovelace",
                  "email":       "long@example.com",
                  "phoneNumber": "+12345678901234567",
                  "dateOfBirth": "1990-12-10",
                  "password":    "Str0ng!Pass"
                }
                """;

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field=='phoneNumber')]").isNotEmpty());
    }

    // =========================================================================
    // AC1 — Missing required fields → 400 with field errors
    // =========================================================================

    @Test
    @DisplayName("AC1 — Missing firstName returns 400 with field error for 'firstName'")
    void ac1_missingFirstName_returns400() throws Exception {
        String body = """
                {
                  "lastName":    "Lovelace",
                  "email":       "nofn@example.com",
                  "phoneNumber": "+14155552671",
                  "dateOfBirth": "1990-12-10",
                  "password":    "Str0ng!Pass"
                }
                """;

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field=='firstName')]").isNotEmpty());
    }

    @Test
    @DisplayName("AC1 — Missing dateOfBirth returns 400 with field error for 'dateOfBirth'")
    void ac1_missingDateOfBirth_returns400() throws Exception {
        String body = """
                {
                  "firstName":   "Ada",
                  "lastName":    "Lovelace",
                  "email":       "nodob@example.com",
                  "phoneNumber": "+14155552671",
                  "password":    "Str0ng!Pass"
                }
                """;

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field=='dateOfBirth')]").isNotEmpty());
    }

    @Test
    @DisplayName("AC1 — Future dateOfBirth returns 400 (@Past violation)")
    void ac1_futureDateOfBirth_returns400() throws Exception {
        String body = """
                {
                  "firstName":   "Ada",
                  "lastName":    "Lovelace",
                  "email":       "future@example.com",
                  "phoneNumber": "+14155552671",
                  "dateOfBirth": "2090-01-01",
                  "password":    "Str0ng!Pass"
                }
                """;

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field=='dateOfBirth')]").isNotEmpty());
    }

    @Test
    @DisplayName("AC1 — Invalid email format returns 400 with field error for 'email'")
    void ac1_invalidEmail_returns400() throws Exception {
        String body = """
                {
                  "firstName":   "Ada",
                  "lastName":    "Lovelace",
                  "email":       "not-an-email",
                  "phoneNumber": "+14155552671",
                  "dateOfBirth": "1990-12-10",
                  "password":    "Str0ng!Pass"
                }
                """;

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field=='email')]").isNotEmpty());
    }

    // =========================================================================
    // AC6 — Response shape and Location header
    // =========================================================================

    @Test
    @DisplayName("AC6 — 201 response contains only 'id'; Location header contains the customer URI")
    void ac6_responseContainsOnlyIdAndLocationHeader() throws Exception {
        String body = mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CustomerFixtures.VALID_REGISTRATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.firstName").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID id = objectMapper.readTree(body).get("id").traverse(objectMapper).readValueAs(UUID.class);
        assertThat(id).isNotNull();

        // Verify the Location header ends with /api/v1/customers/{id}
        String location = mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName":"Bob","lastName":"Builder",
                                  "email":"bob@example.com",
                                  "phoneNumber":"+447700900999",
                                  "dateOfBirth":"1982-03-14",
                                  "password":"Str0ng!Pass"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");

        assertThat(location).isNotNull().contains("/api/v1/customers/");
    }
}
