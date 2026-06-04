// Story: US-006 / US-007 / US-009
package com.northbank.registration.account.repository;

import com.northbank.registration.account.domain.model.AccountType;
import com.northbank.registration.account.domain.model.BankAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Data access for {@link BankAccount} entities.
 *
 * <p>US-009 adds {@link JpaSpecificationExecutor} to support dynamic filter
 * queries in {@code AdminAccountService}.</p>
 */
@Repository
public interface AccountRepository
        extends JpaRepository<BankAccount, UUID>, JpaSpecificationExecutor<BankAccount> {

    /** US-006: Guard against duplicate account type per customer. */
    boolean existsByCustomerIdAndType(UUID customerId, AccountType type);

    /** US-006: Guard against duplicate account number. */
    boolean existsByAccountNumber(String accountNumber);

    /** US-007: All accounts for a customer, newest first. */
    List<BankAccount> findAllByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    // ── US-010 ────────────────────────────────────────────────────────────────

    /**
     * Loads accounts with write locks so transfers can check and mutate balances atomically.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from BankAccount a where a.id in :ids")
    List<BankAccount> findAllByIdInForUpdate(Collection<UUID> ids);
}
