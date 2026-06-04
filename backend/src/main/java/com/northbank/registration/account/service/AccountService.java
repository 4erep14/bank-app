// Story: US-006 + US-008
package com.northbank.registration.account.service;

import com.northbank.registration.account.domain.model.BankAccount;
import com.northbank.registration.account.exception.AccountAccessDeniedException;
import com.northbank.registration.account.exception.AccountNotFoundException;
import com.northbank.registration.account.exception.DuplicateAccountTypeException;
import com.northbank.registration.account.repository.AccountRepository;
import com.northbank.registration.account.service.dto.AccountDetailResponse;
import com.northbank.registration.account.service.dto.AccountSummaryResponse;
import com.northbank.registration.account.service.dto.OpenAccountRequest;
import com.northbank.registration.account.service.dto.OpenAccountResponse;
import com.northbank.registration.audit.domain.model.AuditActionType;
import com.northbank.registration.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

/**
 * Business logic for account management (EPIC-02).
 *
 * <ul>
 *   <li>US-006: Opens a new CHECKING or SAVINGS account.</li>
 *   <li>US-007: Lists all accounts for a customer.</li>
 *   <li>US-008: Returns full details of a single account with ownership check.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private static final int ACCOUNT_NUMBER_LENGTH = 10;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AccountRepository accountRepository;
    private final AuditLogService auditLogService;

    // ── US-006 ───────────────────────────────────────────────────────────────

    /**
     * Opens a new bank account for the given customer.
     *
     * @param customerId authenticated customer UUID (extracted from JWT at controller layer)
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
                .build();

        BankAccount saved = accountRepository.save(account);
        auditLogService.record(AuditActionType.ACCOUNT_OPENED, "ACCOUNT", saved.getId());
        log.info("Account opened: id={}, type={}, customer={}", saved.getId(), saved.getType(), customerId);

        return toResponse(saved);
    }

    // ── US-007 ───────────────────────────────────────────────────────────────

    /**
     * Returns a summary list of all accounts owned by the customer, newest first.
     *
     * @param customerId authenticated customer UUID
     * @return list of account summaries (may be empty)
     */
    @Transactional(readOnly = true)
    public List<AccountSummaryResponse> listAccounts(UUID customerId) {
        log.debug("Listing accounts for customer {}", customerId);
        return accountRepository.findAllByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    // ── US-008 ───────────────────────────────────────────────────────────────

    /**
     * Returns full details of a single account.
     *
     * @param customerId authenticated customer UUID (from JWT)
     * @param accountId  requested account UUID
     * @return account detail view
     * @throws AccountNotFoundException     if no account with accountId exists (AC4)
     * @throws AccountAccessDeniedException if the account belongs to a different customer (AC3)
     */
    @Transactional(readOnly = true)
    public AccountDetailResponse getAccountDetail(UUID customerId, UUID accountId) {
        log.debug("Account detail request: accountId={}, customerId={}", accountId, customerId);

        BankAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        // AC3: ownership check — 403 before leaking any account data
        if (!account.getCustomerId().equals(customerId)) {
            log.warn("Ownership violation: customerId={} attempted access to accountId={}", customerId, accountId);
            throw new AccountAccessDeniedException();
        }

        return toDetail(account);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Generates a unique 10-digit numeric account number.
     * Retries on collision (astronomically unlikely in practice).
     */
    private String generateUniqueAccountNumber() {
        String number;
        int attempts = 0;
        do {
            long firstDigit = 1 + (long) (SECURE_RANDOM.nextDouble() * 9);
            long remaining  = (long) (SECURE_RANDOM.nextDouble() * 1_000_000_000L);
            number = String.format("%d%09d", firstDigit, remaining);
            attempts++;
            if (attempts > 10) {
                throw new IllegalStateException("Unable to generate unique account number after 10 attempts");
            }
        } while (accountRepository.existsByAccountNumber(number));
        return number;
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

    private AccountSummaryResponse toSummary(BankAccount account) {
        return new AccountSummaryResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getType(),
                account.getBalance().setScale(2, RoundingMode.UNNECESSARY),
                account.getStatus()
        );
    }

    private AccountDetailResponse toDetail(BankAccount account) {
        return new AccountDetailResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getType(),
                account.getBalance().setScale(2, RoundingMode.UNNECESSARY),
                account.getStatus(),
                account.getCreatedAt()
        );
    }
}
