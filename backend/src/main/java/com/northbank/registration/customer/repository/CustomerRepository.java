// Story: US-001 / US-002
package com.northbank.registration.customer.repository;

import com.northbank.registration.customer.domain.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Customer} aggregate.
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    /**
     * Existence check used by {@code CustomerService} before persisting a new
     * registration — prevents a friendly 409 for duplicate emails.
     * The DB unique constraint on {@code email} is the race-safe source of truth.
     *
     * @param email lower-cased email address
     * @return {@code true} if a customer with this email already exists
     */
    boolean existsByEmail(String email);

    /**
     * Looks up a customer by their normalised (lower-cased) email.
     * Used by {@code AuthService} (US-002) and {@code PasswordResetService} (US-004).
     *
     * @param email lower-cased, trimmed email address
     * @return {@link Optional} containing the customer, or empty if not found
     */
    Optional<Customer> findByEmail(String email);
}
