// Story: US-006
package com.northbank.registration.account.service;

import com.northbank.registration.account.domain.model.BankAccount;
import com.northbank.registration.account.exception.DuplicateAccountTypeException;
import com.northbank.registration.account.repository.AccountRepository;
import com.northbank.registration.account.service.dto.OpenAccountRequest;
import com.northbank.registration.account.service.dto.OpenAccountResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Business logic for account management (EPIC-02).
 *
 * <p>US-006: Opens a new CHECKING or SAVINGS account for the authenticated customer.
 * All writes run inside a single transaction.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private static final int    ACCOUNT_NUMBER_LENGTH   = 10;
    private static final int    MAX_GENERATION_ATTEMPTS = 10;
    private static final SecureRandom SECURE_RANDOM     = new SecureRandom();

    private final AccountRepository accountRepository;

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

        // AC5: enforce one account per type per customer
        if (accountRepository.existsByCustomerIdAndType(customerId, request.type())) {
            log.warn("Duplicate account type {} attempted by customer {}", request.type(), customerId);
            throw new DuplicateAccountTypeException(request.type());
        }

        String accountNumber = generateUniqueAccountNumber();

        // AC3: balance = 0.00, status = ACTIVE via @Builder.Default
        BankAccount account = BankAccount.builder()
                .accountNumber(accountNumber)
                .type(request.type())
                .customerId(customerId)
                .build();

        BankAccount saved = accountRepository.save(account);
        log.info("Account opened: id={}, type={}, customer={}", saved.getId(), saved.getType(), customerId);

        return toResponse(saved);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Generates a unique 10-digit numeric account number.
     * The first digit is always 1-9 (no leading zeros).
     * Retries on collision (expected to be astronomically rare in practice).
     *
     * @throws IllegalStateException after MAX_GENERATION_ATTEMPTS consecutive collisions
     */
    private String generateUniqueAccountNumber() {
        for (int attempt = 1; attempt <= MAX_GENERATION_ATTEMPTS; attempt++) {
            // First digit: 1-9 to prevent leading zeros
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

    private OpenAccountResponse toResponse(BankAccount account) {
        return new OpenAccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getType(),
                account.getBalance(),
                account.getStatus(),
                account.getCreatedAt()
        );
    }
}
