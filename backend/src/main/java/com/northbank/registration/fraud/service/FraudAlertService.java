// Story: US-017 / US-018
package com.northbank.registration.fraud.service;

import com.northbank.registration.account.domain.model.BankAccount;
import com.northbank.registration.account.repository.AccountRepository;
import com.northbank.registration.audit.domain.model.AuditActionType;
import com.northbank.registration.audit.service.AuditLogService;
import com.northbank.registration.customer.domain.model.Customer;
import com.northbank.registration.customer.repository.CustomerRepository;
import com.northbank.registration.fraud.domain.model.FraudAlert;
import com.northbank.registration.fraud.domain.model.FraudAlertReviewStatus;
import com.northbank.registration.fraud.exception.FraudAlertNotFoundException;
import com.northbank.registration.fraud.exception.InsufficientFundsAtUnblockException;
import com.northbank.registration.fraud.exception.TransactionAlreadyResolvedException;
import com.northbank.registration.fraud.repository.FraudAlertRepository;
import com.northbank.registration.fraud.repository.FraudAlertSpecifications;
import com.northbank.registration.fraud.service.dto.FraudAlertDetailResponse;
import com.northbank.registration.fraud.service.dto.FraudAlertFilter;
import com.northbank.registration.fraud.service.dto.FraudAlertSummaryResponse;
import com.northbank.registration.transaction.domain.model.Transaction;
import com.northbank.registration.transaction.domain.model.TransactionStatus;
import com.northbank.registration.transaction.exception.TransactionNotFoundException;
import com.northbank.registration.transaction.repository.TransactionRepository;
import com.northbank.registration.transaction.service.dto.TransactionDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FraudAlertService {

    private final FraudAlertRepository fraudAlertRepository;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public Page<FraudAlertSummaryResponse> list(FraudAlertFilter filter, Pageable pageable) {
        return fraudAlertRepository.findAll(FraudAlertSpecifications.matches(filter), pageable)
                .map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public FraudAlertDetailResponse get(UUID alertId) {
        FraudAlert alert = fraudAlertRepository.findById(alertId)
                .orElseThrow(FraudAlertNotFoundException::new);
        Transaction transaction = transactionRepository.findById(alert.getTransactionId())
                .orElseThrow(TransactionNotFoundException::new);
        return new FraudAlertDetailResponse(
                alert.getId(),
                toSummary(alert, transaction),
                alert.getThresholdValue(),
                alert.getActualValue(),
                alert.getReviewedBy(),
                alert.getReviewedAt() == null ? null : alert.getReviewedAt().toString(),
                toTransactionDetail(transaction)
        );
    }

    @Transactional
    public FraudAlertDetailResponse approve(UUID alertId, UUID analystId) {
        FraudAlert alert = fraudAlertRepository.findById(alertId)
                .orElseThrow(FraudAlertNotFoundException::new);
        Transaction transaction = transactionRepository.findById(alert.getTransactionId())
                .orElseThrow(TransactionNotFoundException::new);
        ensureBlocked(transaction);

        List<UUID> orderedIds = List.of(transaction.getSourceAccountId(), transaction.getDestinationAccountId())
                .stream()
                .sorted(Comparator.comparing(UUID::toString))
                .toList();
        Map<UUID, BankAccount> accountsById = accountRepository.findAllByIdInForUpdate(orderedIds)
                .stream()
                .collect(Collectors.toMap(BankAccount::getId, Function.identity()));

        BankAccount source = accountsById.get(transaction.getSourceAccountId());
        BankAccount destination = accountsById.get(transaction.getDestinationAccountId());
        if (source == null || destination == null || source.getBalance().compareTo(transaction.getAmount()) < 0) {
            throw new InsufficientFundsAtUnblockException();
        }

        source.setBalance(source.getBalance().subtract(transaction.getAmount()));
        destination.setBalance(destination.getBalance().add(transaction.getAmount()));
        transaction.setStatus(TransactionStatus.COMPLETED);
        markReviewed(alert, analystId);
        auditLogService.record(AuditActionType.TRANSACTION_UNBLOCKED, "TRANSACTION", transaction.getId());

        return get(alert.getId());
    }

    @Transactional
    public FraudAlertDetailResponse reject(UUID alertId, UUID analystId) {
        FraudAlert alert = fraudAlertRepository.findById(alertId)
                .orElseThrow(FraudAlertNotFoundException::new);
        Transaction transaction = transactionRepository.findById(alert.getTransactionId())
                .orElseThrow(TransactionNotFoundException::new);
        ensureBlocked(transaction);

        transaction.setStatus(TransactionStatus.REJECTED);
        markReviewed(alert, analystId);
        auditLogService.record(AuditActionType.TRANSACTION_REJECTED, "TRANSACTION", transaction.getId());

        return get(alert.getId());
    }

    private void ensureBlocked(Transaction transaction) {
        if (transaction.getStatus() != TransactionStatus.BLOCKED) {
            throw new TransactionAlreadyResolvedException();
        }
    }

    private void markReviewed(FraudAlert alert, UUID analystId) {
        alert.setReviewStatus(FraudAlertReviewStatus.REVIEWED);
        alert.setReviewedBy(analystId);
        alert.setReviewedAt(OffsetDateTime.now());
    }

    private FraudAlertSummaryResponse toSummary(FraudAlert alert) {
        Transaction transaction = transactionRepository.findById(alert.getTransactionId())
                .orElseThrow(TransactionNotFoundException::new);
        return toSummary(alert, transaction);
    }

    private FraudAlertSummaryResponse toSummary(FraudAlert alert, Transaction transaction) {
        Customer customer = customerRepository.findById(transaction.getCustomerId()).orElse(null);
        BankAccount sourceAccount = accountRepository.findById(transaction.getSourceAccountId()).orElse(null);
        String customerName = customer == null
                ? "Unknown customer"
                : (customer.getFirstName() + " " + customer.getLastName()).trim();
        String accountNumber = sourceAccount == null ? "Unknown" : sourceAccount.getAccountNumber();

        return new FraudAlertSummaryResponse(
                alert.getId(),
                transaction.getId(),
                transaction.getAmount(),
                customerName,
                accountNumber,
                alert.getRuleName(),
                alert.getRuleConditionType(),
                alert.getTimestamp(),
                alert.getReviewStatus()
        );
    }

    private TransactionDetailResponse toTransactionDetail(Transaction transaction) {
        return new TransactionDetailResponse(
                transaction.getId(),
                transaction.getCustomerId(),
                transaction.getType(),
                transaction.getStatus(),
                transaction.getAmount(),
                transaction.getSourceAccountId(),
                transaction.getDestinationAccountId(),
                transaction.getDescription(),
                transaction.getTimestamp(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt()
        );
    }
}
