// Story: US-006 | US-007
package com.northbank.registration.account.repository;

import com.northbank.registration.account.domain.model.AccountType;
import com.northbank.registration.account.domain.model.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<BankAccount, UUID> {

    // ── US-006 ────────────────────────────────────────────────────────────────

    /**
     * Returns true if the customer already owns an account of the given type.
     * Used for AC5: one CHECKING + one SAVINGS maximum per customer.
     */
    boolean existsByCustomerIdAndType(UUID customerId, AccountType type);

    /**
     * Returns true if the given account number is already taken.
     * Used during unique account number generation to prevent collisions.
     */
    boolean existsByAccountNumber(String accountNumber);

    // ── US-007 ────────────────────────────────────────────────────────────────

    /**
     * Returns all accounts owned by the given customer, newest first.
     *
     * <p>AC3: Only accounts for {@code customerId} are returned.
     * AC4: Returns an empty list when the customer has no accounts.</p>
     */
    List<BankAccount> findAllByCustomerIdOrderByCreatedAtDesc(UUID customerId);
}
