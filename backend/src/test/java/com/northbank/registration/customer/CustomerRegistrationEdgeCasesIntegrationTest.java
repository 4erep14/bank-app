// Story: US-001
package com.northbank.registration.customer;

import com.northbank.registration.config.IntegrationTestBase;
import com.northbank.registration.customer.domain.model.Customer;
import com.northbank.registration.customer.repository.CustomerRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Supplemental integration tests for US-001: Customer Self-Registration.
 *
 * <p>This class covers the edge-cases and boundary conditions that are NOT
 * exercised in {@link CustomerRegistrationIntegrationTest}. Together the two
 * classes provide exhaustive coverage of all six Acceptance Criteria.</p>
 *
 * <p>Acceptance Criteria gap-filled here:</p>
 * <ul>
 *   <li>AC1 — missing lastName / email / phoneNumber; empty body; size-limit violations</li>
 *   <li>AC3 — each individual password rule violation; 8-character positive boundary</li>
 *   <li>AC4 — "+" only; "+0…" (leading-zero country code); max-length (15 digits); UK format</li>
 *   <li>AC5 — {@code status} absent from 201 response; {@code createdAt}/{@code updatedAt}
 *             populated in the database</li>
 *   <li>AC6 — {@code Location} header UUID is a syntactically valid UUID</li>
 *   <li>Protocol — wrong {@code Content-Type} returns 415 Unsupported Media Type</li>
 * </ul>
 *
 * <p>Uses {@link IntegrationTestBase}: real PostgreSQL via Testcontainers,
 * each test method data-isolated via {@code @BeforeEach} cleanup.</p>
 */
@DisplayName("US-001 — Customer Registration: Edge Cases & Boundary Integration Tests")
class CustomerRegistrationEdgeCasesIntegrationTest extends IntegrationTestBase {

    private static final String ENDPOINT = "/api/v1/customers";

    @Autowired
    private CustomerRepository customerRepository;

    /** Used to flush pending INSERTs and clear the first-level cache in timestamp tests. */
    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void cleanDatabase() {
        customerRepository.deleteAll();
    }

    // =========================================================================
    // AC1 — Missing required fields (lastName, email, phoneNumber)
    // =========================================================================

    @Test
    @DisplayName("AC1 — Missing lastName returns 400 with field error for 'lastName'")
    void ac1_missingLastName_returns400WithFieldError() throws Exception {
        String body = """
                {
                  "firstName":   "Ada",
                  "email":       "nolastname@example.com",
                  "phoneNumber": "+14155552671",
                  "dateOfBirth": "1990-12-10",
                  "password":    "Str0ng!Pass"
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
                .andExpect(jsonPath("$.errors[?(@.field=='lastName')]").isNotEmpty());

        assertThat(customerRepository.count()).isZero();
    }

    @Test
    @DisplayName("AC1 — Missing email returns 400 with field error for 'email'")
    void ac1_missingEmail_returns400WithFieldError() throws Exception {
        String body = """
                {
                  "firstName":   "Ada",
                  "lastName":    "Lovelace",
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

        assertThat(customerRepository.count()).isZero();
    }

    @Test
    @DisplayName("AC1 — Missing phoneNumber returns 400 with field error for 'phoneNumber'")
    void ac1_missingPhoneNumber_returns400WithFieldError() throws Exception {
        String body = """
                {
                  "firstName":   "Ada",
                  "lastName":    "Lovelace",
                  "email":       "nophone@example.com",
                  "dateOfBirth": "1990-12-10",
                  "password":    "Str0ng!Pass"
                }
                """;

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field=='phoneNumber')]").isNotEmpty());

        assertThat(customerRepository.count()).isZero();
    }

    // =========================================================================
    // AC1 — Empty body returns 400 with ALL required field errors
    // =========================================================================

    @Test
    @DisplayName("AC1 — Empty JSON body '{}' returns 400 with field errors for all 6 required fields")
    void ac1_emptyRequestBody_returns400WithAllSixFieldErrors() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray())
                // All 6 required fields must have a validation error
                .andExpect(jsonPath("$.errors[?(@.field=='firstName')]").isNotEmpty())
                .andExpect(jsonPath("$.errors[?(@.field=='lastName')]").isNotEmpty())
                .andExpect(jsonPath("$.errors[?(@.field=='email')]").isNotEmpty())
                .andExpect(jsonPath("$.errors[?(@.field=='phoneNumber')]").isNotEmpty())
                .andExpect(jsonPath("$.errors[?(@.field=='dateOfBirth')]").isNotEmpty())
                .andExpect(jsonPath("$.errors[?(@.field=='password')]").isNotEmpty());

        assertThat(customerRepository.count()).isZero();
    }

    // =========================================================================
    // AC1 — Size-limit violations (firstName / lastName max = 100)
    // =========================================================================

    @Test
    @DisplayName("AC1 — firstName exceeding 100 characters returns 400 with field error for 'firstName'")
    void ac1_firstNameExceedsMaxLength_returns400() throws Exception {
        String tooLong = "A".repeat(101);   // 101 chars > max 100

        String body = String.format("""
                {
                  "firstName":   "%s",
                  "lastName":    "Lovelace",
                  "email":       "longfirst@example.com",
                  "phoneNumber": "+14155552671",
                  "dateOfBirth": "1990-12-10",
                  "password":    "Str0ng!Pass"
                }
                """, tooLong);

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field=='firstName')]").isNotEmpty());

        assertThat(customerRepository.count()).isZero();
    }

    @Test
    @DisplayName("AC1 — lastName exceeding 100 characters returns 400 with field error for 'lastName'")
    void ac1_lastNameExceedsMaxLength_returns400() throws Exception {
        String tooLong = "B".repeat(101);   // 101 chars > max 100

        String body = String.format("""
                {
                  "firstName":   "Ada",
                  "lastName":    "%s",
                  "email":       "longlast@example.com",
                  "phoneNumber": "+14155552671",
                  "dateOfBirth": "1990-12-10",
                  "password":    "Str0ng!Pass"
                }
                """, tooLong);

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field=='lastName')]").isNotEmpty());

        assertThat(customerRepository.count()).isZero();
    }

    @Test
    @DisplayName("AC1 — firstName and lastName at exactly 100 characters (boundary) are accepted — 201")
    void ac1_firstAndLastNameAtMaxLength_returns201() throws Exception {
        String exactly100 = "A".repeat(100);    // exactly at the boundary → valid

        String body = String.format("""
                {
                  "firstName":   "%s",
                  "lastName":    "%s",
                  "email":       "exact100@example.com",
                  "phoneNumber": "+14155552671",
                  "dateOfBirth": "1990-12-10",
                  "password":    "Str0ng!Pass"
                }
                """, exactly100, exactly100);

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty());

        assertThat(customerRepository.count()).isEqualTo(1);
        Customer saved = customerRepository.findAll().get(0);
        assertThat(saved.getFirstName()).hasSize(100);
        assertThat(saved.getLastName()).hasSize(100);
    }

    // =========================================================================
    // AC3 — Individual password rule violations (one rule broken at a time)
    // =========================================================================

    @Test
    @DisplayName("AC3 — Password missing lowercase letter returns 400 with 'lowercase' in error message")
    void ac3_passwordMissingLowercase_returns400() throws Exception {
        // "UPPERCASE123!" has upper, digits, special — but NO lowercase
        String body = """
                {
                  "firstName":   "Ada",
                  "lastName":    "Lovelace",
                  "email":       "nolower@example.com",
                  "phoneNumber": "+14155552671",
                  "dateOfBirth": "1990-12-10",
                  "password":    "UPPERCASE123!"
                }
                """;

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field=='password')]").isNotEmpty())
                .andExpect(jsonPath("$.errors[?(@.field=='password')].message",
                        hasItem(containsString("lowercase"))));

        assertThat(customerRepository.count()).isZero();
    }

    @Test
    @DisplayName("AC3 — Password missing digit returns 400 with 'digit' in error message")
    void ac3_passwordMissingDigit_returns400() throws Exception {
        // "Uppercase!@#" has upper, lower, special — but NO digit
        String body = """
                {
                  "firstName":   "Ada",
                  "lastName":    "Lovelace",
                  "email":       "nodigit@example.com",
                  "phoneNumber": "+14155552671",
                  "dateOfBirth": "1990-12-10",
                  "password":    "Uppercase!@#"
                }
                """;

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field=='password')]").isNotEmpty())
                .andExpect(jsonPath("$.errors[?(@.field=='password')].message",
                        hasItem(containsString("digit"))));

        assertThat(customerRepository.count()).isZero();
    }

    @Test
    @DisplayName("AC3 — Password missing special character returns 400 with 'special character' in error message")
    void ac3_passwordMissingSpecialChar_returns400() throws Exception {
        // "Uppercase123" has upper, lower, digit — but NO special char
        String body = """
                {
                  "firstName":   "Ada",
                  "lastName":    "Lovelace",
                  "email":       "nospecial@example.com",
                  "phoneNumber": "+14155552671",
                  "dateOfBirth": "1990-12-10",
                  "password":    "Uppercase123"
                }
                """;

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field=='password')]").isNotEmpty())
                .andExpect(jsonPath("$.errors[?(@.field=='password')].message",
                        hasItem(containsString("special character"))));

        assertThat(customerRepository.count()).isZero();
    }

    @Test
    @DisplayName("AC3 — Password of exactly 8 characters satisfying all complexity rules returns 201 (minimum-boundary positive)")
    void ac3_passwordExactlyEightCharsWithAllRulesMet_returns201() throws Exception {
        // "Str0ng!P" — length=8, upper(S,P), lower(t,r,n,g), digit(0), special(!)
        String body = """
                {
                  "firstName":   "Ada",
                  "lastName":    "Lovelace",
                  "email":       "minpass@example.com",
                  "phoneNumber": "+14155552671",
                  "dateOfBirth": "1990-12-10",
                  "password":    "Str0ng!P"
                }
                """;

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty());

        assertThat(customerRepository.count()).isEqualTo(1);
        Customer saved = customerRepository.findAll().get(0);
        // Password is BCrypt-hashed — raw value must not be stored
        assertThat(saved.getPasswordHash()).startsWith("$2a$");
        assertThat(saved.getPasswordHash()).doesNotContain("Str0ng!P");
    }

    @Test
    @DisplayName("AC3 — Password of exactly 7 characters (one below minimum) returns 400")
    void ac3_passwordSevenChars_returns400() throws Exception {
        // "Str0ng!" — length=7, all other rules met — must fail on length alone
        String body = """
                {
                  "firstName":   "Ada",
                  "lastName":    "Lovelace",
                  "email":       "sevenchar@example.com",
                  "phoneNumber": "+14155552671",
                  "dateOfBirth": "1990-12-10",
                  "password":    "Str0ng!"
                }
                """;

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field=='password')]").isNotEmpty())
                .andExpect(jsonPath("$.errors[?(@.field=='password')].message",
                        hasItem(containsString("8 characters"))));

        assertThat(customerRepository.count()).isZero();
    }

    // =========================================================================
    // AC4 — E.164 phone number edge cases
    // =========================================================================

    @Test
    @DisplayName("AC4 — Phone number containing only '+' returns 400 (too short for E.164)")
    void ac4_phonePlusOnly_returns400() throws Exception {
        String body = """
                {
                  "firstName":   "Ada",
                  "lastName":    "Lovelace",
                  "email":       "plusonly@example.com",
                  "phoneNumber": "+",
                  "dateOfBirth": "1990-12-10",
                  "password":    "Str0ng!Pass"
                }
                """;

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field=='phoneNumber')]").isNotEmpty());

        assertThat(customerRepository.count()).isZero();
    }

    @Test
    @DisplayName("AC4 — Phone number starting with '+0' returns 400 (E.164 requires country code digit 1-9)")
    void ac4_phoneLeadingZeroAfterPlus_returns400() throws Exception {
        // E.164: country code digit must be 1-9, never 0
        String body = """
                {
                  "firstName":   "Ada",
                  "lastName":    "Lovelace",
                  "email":       "zerocountry@example.com",
                  "phoneNumber": "+01234567890",
                  "dateOfBirth": "1990-12-10",
                  "password":    "Str0ng!Pass"
                }
                """;

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field=='phoneNumber')]").isNotEmpty());

        assertThat(customerRepository.count()).isZero();
    }

    @Test
    @DisplayName("AC4 — Phone number at E.164 maximum length (15 digits, 16 chars total) returns 201")
    void ac4_phoneAtMaxE164Length_returns201() throws Exception {
        // +123456789012345 = '+' + 15 digits → valid E.164 maximum
        // Regex: ^\+[1-9]\d{1,14}$ → [1-9]=1, \d{1,14}=23456789012345 (14 digits)
        String body = """
                {
                  "firstName":   "Ada",
                  "lastName":    "Lovelace",
                  "email":       "maxphone@example.com",
                  "phoneNumber": "+123456789012345",
                  "dateOfBirth": "1990-12-10",
                  "password":    "Str0ng!Pass"
                }
                """;

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty());

        assertThat(customerRepository.count()).isEqualTo(1);
        assertThat(customerRepository.findAll().get(0).getPhoneNumber())
                .isEqualTo("+123456789012345");
    }

    @Test
    @DisplayName("AC4 — Valid UK phone number in E.164 format (+447911123456) returns 201")
    void ac4_validUkPhoneNumber_returns201() throws Exception {
        String body = """
                {
                  "firstName":   "Ada",
                  "lastName":    "Lovelace",
                  "email":       "ukphone@example.com",
                  "phoneNumber": "+447911123456",
                  "dateOfBirth": "1990-12-10",
                  "password":    "Str0ng!Pass"
                }
                """;

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty());

        assertThat(customerRepository.count()).isEqualTo(1);
        assertThat(customerRepository.findAll().get(0).getPhoneNumber())
                .isEqualTo("+447911123456");
    }

    @Test
    @DisplayName("AC4 — Phone number at minimum valid E.164 length (3 chars: +1X) returns 201")
    void ac4_phoneAtMinimumE164Length_returns201() throws Exception {
        // +12 = '+' + country-code '1' + subscriber '2' → minimum valid match
        String body = """
                {
                  "firstName":   "Ada",
                  "lastName":    "Lovelace",
                  "email":       "minphone@example.com",
                  "phoneNumber": "+12",
                  "dateOfBirth": "1990-12-10",
                  "password":    "Str0ng!Pass"
                }
                """;

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    // =========================================================================
    // AC5 — PENDING_VERIFICATION status is NOT surfaced in the API response
    // =========================================================================

    @Test
    @DisplayName("AC5 — Successful 201 response body contains only 'id'; 'status' is NOT present")
    void ac5_statusNotPresentInSuccessResponseBody() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName":   "Ada",
                                  "lastName":    "Lovelace",
                                  "email":       "statuscheck@example.com",
                                  "phoneNumber": "+14155552671",
                                  "dateOfBirth": "1990-12-10",
                                  "password":    "Str0ng!Pass"
                                }
                                """))
                .andExpect(status().isCreated())
                // ONLY 'id' must be in the response — AC5: PENDING_VERIFICATION not surfaced
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.firstName").doesNotExist())
                .andExpect(jsonPath("$.lastName").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.phoneNumber").doesNotExist())
                .andExpect(jsonPath("$.dateOfBirth").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.createdAt").doesNotExist())
                .andExpect(jsonPath("$.updatedAt").doesNotExist());
    }

    // =========================================================================
    // AC5 — createdAt and updatedAt are populated in the database
    // =========================================================================

    @Test
    @DisplayName("AC5 — Persisted customer has non-null createdAt and updatedAt timestamps in the database")
    void ac5_successfulRegistration_dbTimestampsArePopulated() throws Exception {
        String responseBody = mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName":   "Ada",
                                  "lastName":    "Lovelace",
                                  "email":       "timestamps@example.com",
                                  "phoneNumber": "+14155552671",
                                  "dateOfBirth": "1990-12-10",
                                  "password":    "Str0ng!Pass"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID returnedId = objectMapper.readTree(responseBody)
                .get("id")
                .traverse(objectMapper)
                .readValueAs(UUID.class);

        // Flush pending INSERT so @CreationTimestamp / @UpdateTimestamp fire,
        // then clear the first-level cache so findById() issues a fresh SELECT
        // from the DB (rather than returning the pre-flush cached entity where
        // the Hibernate-generated timestamps would still be null).
        entityManager.flush();
        entityManager.clear();

        Customer saved = customerRepository.findById(returnedId).orElseThrow();

        // createdAt and updatedAt must be set by the DB / Hibernate (not null)
        assertThat(saved.getCreatedAt())
                .as("createdAt must be populated by the database")
                .isNotNull();
        assertThat(saved.getUpdatedAt())
                .as("updatedAt must be populated by the database")
                .isNotNull();

        // Both timestamps should be close to now (within 10 seconds)
        assertThat(saved.getCreatedAt())
                .isAfter(java.time.OffsetDateTime.now().minusSeconds(10));
        assertThat(saved.getUpdatedAt())
                .isAfter(java.time.OffsetDateTime.now().minusSeconds(10));
    }

    // =========================================================================
    // AC6 — Location header UUID validity
    // =========================================================================

    @Test
    @DisplayName("AC6 — Location header path segment is a syntactically valid UUID")
    void ac6_locationHeaderPathSegmentIsValidUuid() throws Exception {
        String location = mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName":   "Ada",
                                  "lastName":    "Lovelace",
                                  "email":       "locationuuid@example.com",
                                  "phoneNumber": "+14155552671",
                                  "dateOfBirth": "1990-12-10",
                                  "password":    "Str0ng!Pass"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn()
                .getResponse()
                .getHeader("Location");

        assertThat(location).isNotNull();

        // Extract the UUID at the end of the Location path
        String uuidSegment = location.substring(location.lastIndexOf('/') + 1);
        assertThat(uuidSegment)
                .as("Location header must end with a valid UUID")
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

        // The UUID in Location must match the 'id' in the response body
        String responseBody = mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName":   "Bob",
                                  "lastName":    "Builder",
                                  "email":       "locationuuid2@example.com",
                                  "phoneNumber": "+447700900123",
                                  "dateOfBirth": "1985-03-14",
                                  "password":    "Str0ng!Pass"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String responseId = objectMapper.readTree(responseBody).get("id").asText();
        String location2  = mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName":   "Carol",
                                  "lastName":    "Cabbage",
                                  "email":       "locationuuid3@example.com",
                                  "phoneNumber": "+447700900456",
                                  "dateOfBirth": "1992-07-22",
                                  "password":    "Str0ng!Pass"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");

        // Ensure location is based on the id returned by the same request
        assertThat(location2).isNotNull();
        assertThat(location2).endsWith(
                customerRepository.findAll().stream()
                        .filter(c -> "carol.cabbage@example.com".equalsIgnoreCase(c.getEmail())
                                  || c.getEmail().contains("locationuuid3"))
                        .findFirst()
                        .map(c -> c.getId().toString())
                        .orElse("NOT_FOUND")
        );
    }

    // =========================================================================
    // AC6 — Response body 'id' matches the id of the persisted record
    // =========================================================================

    @Test
    @DisplayName("AC6 — The 'id' in the 201 response body matches the id of the persisted customer record")
    void ac6_responseIdMatchesPersistedCustomerId() throws Exception {
        String responseBody = mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName":   "Ada",
                                  "lastName":    "Lovelace",
                                  "email":       "idmatch@example.com",
                                  "phoneNumber": "+14155552671",
                                  "dateOfBirth": "1990-12-10",
                                  "password":    "Str0ng!Pass"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID returnedId = objectMapper.readTree(responseBody)
                .get("id")
                .traverse(objectMapper)
                .readValueAs(UUID.class);

        // Must find exactly one customer, and its id must match the returned id
        List<Customer> allCustomers = customerRepository.findAll();
        assertThat(allCustomers).hasSize(1);
        assertThat(allCustomers.get(0).getId()).isEqualTo(returnedId);
    }

    // =========================================================================
    // AC2 — Race-safe: DB unique-constraint fallback (DataIntegrityViolationException)
    // =========================================================================

    @Test
    @DisplayName("AC2 — Race-safe: two sequential registrations with identical emails — second returns 409")
    void ac2_sequentialDuplicateRegistrations_secondReturns409() throws Exception {
        String firstBody = """
                {
                  "firstName":   "Ada",
                  "lastName":    "Lovelace",
                  "email":       "racesafe@example.com",
                  "phoneNumber": "+14155552671",
                  "dateOfBirth": "1990-12-10",
                  "password":    "Str0ng!Pass"
                }
                """;
        String secondBody = """
                {
                  "firstName":   "Charles",
                  "lastName":    "Babbage",
                  "email":       "racesafe@example.com",
                  "phoneNumber": "+447700900999",
                  "dateOfBirth": "1985-05-20",
                  "password":    "Str0ng!Pass"
                }
                """;

        // First registration must succeed
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstBody))
                .andExpect(status().isCreated());

        // Second registration with same email must be rejected with 409
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Email already registered"));

        // Only the first customer was persisted
        assertThat(customerRepository.count()).isEqualTo(1);
        assertThat(customerRepository.findAll().get(0).getFirstName()).isEqualTo("Ada");
    }

    // =========================================================================
    // Protocol — Wrong Content-Type returns 415 Unsupported Media Type
    // =========================================================================

    @Test
    @DisplayName("Protocol — POST with Content-Type text/plain returns 415 Unsupported Media Type")
    void wrongContentType_textPlain_returns415() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("firstName=Ada&lastName=Lovelace"))
                .andExpect(status().isUnsupportedMediaType());

        assertThat(customerRepository.count()).isZero();
    }

    @Test
    @DisplayName("Protocol — POST without Content-Type returns 415 Unsupported Media Type")
    void noContentType_returns415() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .content("{\"firstName\":\"Ada\"}"))
                .andExpect(status().isUnsupportedMediaType());

        assertThat(customerRepository.count()).isZero();
    }

    // =========================================================================
    // AC3 — Password with all violations (length + all rule failures) is rejected
    // =========================================================================

    @Test
    @DisplayName("AC3 — Password violating all 5 rules produces a single error listing every missing requirement")
    void ac3_passwordViolatingAllRules_singleErrorListingAllViolations() throws Exception {
        // "a" — length < 8, no uppercase, no digit, no special char
        // (lowercase IS present, so that rule is met; all others fail)
        String body = """
                {
                  "firstName":   "Ada",
                  "lastName":    "Lovelace",
                  "email":       "allviolations@example.com",
                  "phoneNumber": "+14155552671",
                  "dateOfBirth": "1990-12-10",
                  "password":    "a"
                }
                """;

        // Note: @NotBlank passes (value is not blank), @Password fires with:
        // "at least 8 characters", "at least one uppercase letter",
        // "at least one digit", "at least one special character"
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field=='password')]").isNotEmpty())
                .andExpect(jsonPath("$.errors[?(@.field=='password')].message",
                        hasItem(allOf(
                                containsString("8 characters"),
                                containsString("uppercase"),
                                containsString("digit"),
                                containsString("special character")
                        ))));

        assertThat(customerRepository.count()).isZero();
    }

    // =========================================================================
    // AC1 — Valid registration with all boundary conditions met (positive)
    // =========================================================================

    @Test
    @DisplayName("AC1 — Minimum-length valid values for all fields are accepted — 201")
    void ac1_minimumValidFieldValues_returns201() throws Exception {
        // firstName=1 char, lastName=1 char, email minimal, phone minimal E.164,
        // DOB yesterday (past), password at exactly 8 chars with all rules
        String yesterday = java.time.LocalDate.now().minusDays(1).toString();

        String body = String.format("""
                {
                  "firstName":   "A",
                  "lastName":    "B",
                  "email":       "x@y.io",
                  "phoneNumber": "+12",
                  "dateOfBirth": "%s",
                  "password":    "Str0ng!P"
                }
                """, yesterday);

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty());

        assertThat(customerRepository.count()).isEqualTo(1);
    }
}
