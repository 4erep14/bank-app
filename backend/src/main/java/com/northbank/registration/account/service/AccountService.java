// Story: US-006 | US-007
package com.northbank.registration.account.service;

import com.northbank.registration.account.domain.model.BankAccount;
import com.northbank.registration.account.exception.DuplicateAccountTypeException;
import com.northbank.registration.account.repository.AccountRepository;
import com.northbank.registration.account.service.dto.AccountSummaryResponse;
import com.northbank.registration.account.service.dto.OpenAccountRequest;
import com.northbank.registration.account.service.dto.OpenAccountResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

/**
 * Business logic for account management (EPIC-02).
 *
 * <ul>
 *   <li>US-006: Opens a new CHECKING or SAVINGS account.</li>
 *   <li>US-007: Lists all accounts for an authenticated customer.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private static final int          MAX_GENERATION_ATTEMPTS = 10;
    private static final SecureRandom SECURE_RANDOM           = new SecureRandom();

    private final AccountRepository accountRepository;

    // ── US-006 ────────────────────────────────────────────────────────────────

    /**
     * Opens a new bank account for the given customer.
     *
     * @param customerId authenticated customer's UUID (extracted from JWT at controller layer)
     * @param request    contains the desired account type
     * @return the newly created account details
     * @throws DuplicateAccountTypeException if the customer already owns an account of this type (AC5)
     */
    @Transactional
    public OpenAccountResponse openAccount(UUID customerId, OpenAccountRequest request) {
        log.debug("Opening {} account for customer {}", request.type(), customerId);

        if (accountRepository.existsByCustomerIdAndType(customerId, request.type())) {
            log.warn("Duplicate account type {} attempted by customer {}", request.type(), customerId);
            throw new DuplicateAccountTypeException(request.type());
        }

        String accountNumber = generateUniqueAccountNumber();

        BankAccount account = BankAccount.builder()
                .accountNumber(accountNumber)
                .type(request.type())
                .customerId(customerId)
                .build(); // balance=0.00, status=ACTIVE via @Builder.Default

        BankAccount saved = accountRepository.save(account);
        log.info("Account opened: id={}, type={}, customer={}", saved.getId(), saved.getType(), customerId);

        return toOpenResponse(saved);
    }

    // ── US-007 ────────────────────────────────────────────────────────────────

    /**
     * Returns all accounts for the authenticated customer.
     *
     * <p>AC3: Scoped strictly to {@code customerId} — no cross-customer leakage.<br>
     * AC4: Returns an empty list when the customer has no accounts.<br>
     * AC5: Balance is scaled to exactly 2 decimal places before serialization.</p>
     *
     * @param customerId extracted from JWT at controller layer
     * @return list of account summaries, ordered newest-first
     */
    @Transactional(readOnly = true)
    public List<AccountSummaryResponse> listAccounts(UUID customerId) {
        log.debug("Listing accounts for customer {}", customerId);

        return accountRepository
                .findAllByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Generates a unique 10-digit numeric account number.
     * First digit is always 1-9 (no leading zeros).
     *
     * @throws IllegalStateException after MAX_GENERATION_ATTEMPTS consecutive collisions
     */
    private String generateUniqueAccountNumber() {
        for (int attempt = 1; attempt <= MAX_GENERATION_ATTEMPTS; attempt++) {
            long firstDigit = 1L + (long) (SECURE_RANDOM.nextDouble() * 9);
            long remaining  = (long) (SECURE_RANDOM.nextDouble() * 1_000_000_000L);
            String candidate = String.format("%d%09d", firstDigit, remaining);

            if (!accountRepository.existsByAccountNumber(candidate)) {
                return candidate;
            }
            log.warn("Account number collision on attempt {}/{}", attempt, MAX_GENERATION_ATTEMPTS);
        }
        throw new IllegalStateException(
                "Unable to generate a unique account number after " + MAX_GENERATION_ATTEMPTS + " attempts");
    }

    private OpenAccountResponse toOpenResponse(BankAccount account) {
        return new OpenAccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getType(),
                account.getBalance(),
                account.getStatus(),
                account.getCreatedAt()
        );
    }

    private AccountSummaryResponse toSummary(BankAccount account) {
        return new AccountSummaryResponse(
                account.getAccountNumber(),
                account.getType(),
                account.getBalance().setScale(2, RoundingMode.UNNECESSARY), // AC5
                account.getStatus()
        );
    }
}
