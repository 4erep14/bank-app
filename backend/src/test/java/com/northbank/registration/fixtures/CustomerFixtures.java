// Story: US-001
package com.northbank.registration.fixtures;

import com.northbank.registration.customer.domain.model.Customer;
import com.northbank.registration.customer.domain.model.CustomerStatus;
import com.northbank.registration.customer.repository.CustomerRepository;

import java.time.LocalDate;

/**
 * Test data factory for {@link Customer} objects used in US-001 integration tests.
 * Keeps test setup declarative and avoids duplication across test classes.
 */
public final class CustomerFixtures {

    private CustomerFixtures() {}

    /** A valid JSON registration request body — all fields satisfy AC1–AC4. */
    public static final String VALID_REGISTRATION_JSON = """
            {
              "firstName":   "Ada",
              "lastName":    "Lovelace",
              "email":       "ada.lovelace@example.com",
              "phoneNumber": "+14155552671",
              "dateOfBirth": "1990-12-10",
              "password":    "Str0ng!Pass"
            }
            """;

    /** The normalised (lower-cased) email used in {@link #VALID_REGISTRATION_JSON}. */
    public static final String VALID_EMAIL = "ada.lovelace@example.com";

    /**
     * Persists a minimal Customer directly via the repository so tests that
     * need a pre-existing record (e.g. 409 duplicate tests) can set up state
     * without going through the HTTP layer.
     *
     * @param repository target repository
     * @param email      lower-cased email to use for the pre-existing record
     * @return the saved {@link Customer}
     */
    public static Customer persistCustomer(CustomerRepository repository, String email) {
        Customer customer = Customer.builder()
                .firstName("Existing")
                .lastName("User")
                .email(email.toLowerCase())
                .phoneNumber("+441234567890")
                .dateOfBirth(LocalDate.of(1985, 6, 15))
                .passwordHash("$2a$12$hashedpassword00000000000000000000000000000000000000000")
                .status(CustomerStatus.PENDING_VERIFICATION)
                .build();
        return repository.save(customer);
    }
}
