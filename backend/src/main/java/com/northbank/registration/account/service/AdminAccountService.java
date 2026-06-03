// Story: US-009
package com.northbank.registration.account.service;

import com.northbank.registration.account.domain.model.AccountStatus;
import com.northbank.registration.account.domain.model.AccountType;
import com.northbank.registration.account.domain.model.BankAccount;
import com.northbank.registration.account.exception.AccountNotFoundException;
import com.northbank.registration.account.repository.AccountRepository;
import com.northbank.registration.account.service.dto.AdminAccountSummaryResponse;
import com.northbank.registration.customer.domain.model.Customer;
import com.northbank.registration.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Business logic for admin account management (US-009).
 *
 * <p>All methods require the caller to already hold {@code ROLE_ADMIN}
 * — enforcement is at the controller/security layer, not here.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAccountService {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final AccountRepository  accountRepository;
    private final CustomerRepository customerRepository;

    // ── AC1 + AC3: Paginated list with optional filters ──────────────────────

    /**
     * Returns a paginated list of all accounts with optional filters (AC1, AC3).
     *
     * @param customerId filter by owner UUID (nullable)
     * @param type       filter by account type (nullable)
     * @param status     filter by account status (nullable)
     * @param page       zero-based page index
     * @param size       page size (defaults to 20 if null or <= 0)
     */
    @Transactional(readOnly = true)
    public Page<AdminAccountSummaryResponse> listAccounts(
            UUID          customerId,
            AccountType   type,
            AccountStatus status,
            int           page,
            int           size) {

        int resolvedSize = (size > 0) ? size : DEFAULT_PAGE_SIZE;
        Pageable pageable = PageRequest.of(page, resolvedSize,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        // Build a dynamic Specification so all filter combinations work
        Specification<BankAccount> spec = Specification.where(null);
        if (customerId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("customerId"), customerId));
        }
        if (type != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("type"), type));
        }
        if (status != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), status));
        }

        Page<BankAccount> accountPage = accountRepository.findAll(spec, pageable);

        // Batch-load owners in one query to avoid N+1
        Set<UUID> customerIds = accountPage.stream()
                .map(BankAccount::getCustomerId)
                .collect(Collectors.toSet());
        Map<UUID, Customer> customerMap = customerRepository.findAllById(customerIds)
                .stream()
                .collect(Collectors.toMap(Customer::getId, c -> c));

        return accountPage.map(account -> toAdminSummary(account, customerMap));
    }

    // ── AC4: Deactivate ───────────────────────────────────────────────────────

    /**
     * Sets an ACTIVE account to INACTIVE (AC4).
     *
     * @throws AccountNotFoundException        if accountId does not exist
     * @throws IllegalStateException           if the account is already INACTIVE
     */
    @Transactional
    public AdminAccountSummaryResponse deactivateAccount(UUID accountId) {
        BankAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        if (account.getStatus() == AccountStatus.INACTIVE) {
            throw new IllegalStateException("Account is already inactive");
        }

        log.info("Admin deactivating accountId={}", accountId);
        account.setStatus(AccountStatus.INACTIVE);
        BankAccount saved = accountRepository.save(account);

        Customer owner = customerRepository.findById(saved.getCustomerId()).orElse(null);
        return toAdminSummaryDirect(saved, owner);
    }

    // ── AC5: Activate ─────────────────────────────────────────────────────────

    /**
     * Sets an INACTIVE account back to ACTIVE (AC5).
     *
     * @throws AccountNotFoundException        if accountId does not exist
     * @throws IllegalStateException           if the account is not INACTIVE
     */
    @Transactional
    public AdminAccountSummaryResponse activateAccount(UUID accountId) {
        BankAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        if (account.getStatus() != AccountStatus.INACTIVE) {
            throw new IllegalStateException("Account is not inactive");
        }

        log.info("Admin activating accountId={}", accountId);
        account.setStatus(AccountStatus.ACTIVE);
        BankAccount saved = accountRepository.save(account);

        Customer owner = customerRepository.findById(saved.getCustomerId()).orElse(null);
        return toAdminSummaryDirect(saved, owner);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private AdminAccountSummaryResponse toAdminSummary(
            BankAccount account, Map<UUID, Customer> customerMap) {
        Customer owner = customerMap.get(account.getCustomerId());
        return toAdminSummaryDirect(account, owner);
    }

    private AdminAccountSummaryResponse toAdminSummaryDirect(
            BankAccount account, Customer owner) {
        String fullName = (owner != null)
                ? owner.getFirstName() + " " + owner.getLastName()
                : "Unknown";
        String email = (owner != null) ? owner.getEmail() : "unknown";
        return new AdminAccountSummaryResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getType(),
                account.getBalance().setScale(2, RoundingMode.HALF_UP),
                account.getStatus(),
                fullName,
                email
        );
    }
}
