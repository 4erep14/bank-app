// Story: US-006
package com.northbank.registration.account.repository;

import com.northbank.registration.account.domain.model.AccountType;
import com.northbank.registration.account.domain.model.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<BankAccount, UUID> {

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
}
