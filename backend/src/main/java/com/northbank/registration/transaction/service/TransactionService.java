// Story: US-010
package com.northbank.registration.transaction.service;

import com.northbank.registration.account.domain.model.AccountStatus;
import com.northbank.registration.account.domain.model.BankAccount;
import com.northbank.registration.account.repository.AccountRepository;
import com.northbank.registration.transaction.domain.event.FraudEvaluationRequestedEvent;
import com.northbank.registration.transaction.domain.model.Transaction;
import com.northbank.registration.transaction.domain.model.TransactionStatus;
import com.northbank.registration.transaction.domain.model.TransactionType;
import com.northbank.registration.transaction.exception.InactiveAccountException;
import com.northbank.registration.transaction.exception.InsufficientFundsException;
import com.northbank.registration.transaction.exception.SameAccountTransferException;
import com.northbank.registration.transaction.exception.TransactionNotFoundException;
import com.northbank.registration.transaction.exception.TransferAccountAccessDeniedException;
import com.northbank.registration.transaction.repository.TransactionRepository;
import com.northbank.registration.transaction.repository.TransactionSpecifications;
import com.northbank.registration.transaction.service.dto.TransactionDetailResponse;
import com.northbank.registration.transaction.service.dto.TransactionFilter;
import com.northbank.registration.transaction.service.dto.TransactionSummaryResponse;
import com.northbank.registration.transaction.service.dto.TransferRequest;
import com.northbank.registration.transaction.service.dto.TransferResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final FraudEvaluationPort fraudEvaluationPort;

    @Transactional(readOnly = true)
    public Page<TransactionSummaryResponse> listCustomerTransactions(UUID customerId, TransactionFilter filter, Pageable pageable) {
        return transactionRepository.findAll(TransactionSpecifications.matches(filter, customerId), pageable)
                .map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public TransactionDetailResponse getCustomerTransaction(UUID customerId, UUID transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(TransactionNotFoundException::new);
        if (!customerId.equals(transaction.getCustomerId())) {
            throw new TransactionNotFoundException();
        }
        return toDetail(transaction);
    }

    @Transactional(readOnly = true)
    public Page<TransactionSummaryResponse> listAdminTransactions(TransactionFilter filter, Pageable pageable) {
        return transactionRepository.findAll(TransactionSpecifications.matches(filter, null), pageable)
                .map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public TransactionDetailResponse getAdminTransaction(UUID transactionId) {
        return transactionRepository.findById(transactionId)
                .map(this::toDetail)
                .orElseThrow(TransactionNotFoundException::new);
    }

    @Transactional
    public TransferResponse transfer(UUID customerId, TransferRequest request) {
        UUID sourceId = request.sourceAccountId();
        UUID destinationId = request.destinationAccountId();

        if (sourceId.equals(destinationId)) {
            throw new SameAccountTransferException();
        }

        BigDecimal amount = request.amount().setScale(2, RoundingMode.UNNECESSARY);
        List<UUID> orderedIds = List.of(sourceId, destinationId).stream()
                .sorted(Comparator.comparing(UUID::toString))
                .toList();

        Map<UUID, BankAccount> accountsById = accountRepository.findAllByIdInForUpdate(orderedIds)
                .stream()
                .collect(Collectors.toMap(BankAccount::getId, Function.identity()));

        BankAccount source = accountsById.get(sourceId);
        BankAccount destination = accountsById.get(destinationId);

        if (source == null || destination == null ||
                !customerId.equals(source.getCustomerId()) ||
                !customerId.equals(destination.getCustomerId())) {
            log.warn("Transfer ownership violation: customerId={}, sourceId={}, destinationId={}",
                    customerId, sourceId, destinationId);
            throw new TransferAccountAccessDeniedException();
        }

        if (source.getStatus() != AccountStatus.ACTIVE || destination.getStatus() != AccountStatus.ACTIVE) {
            throw new InactiveAccountException();
        }

        if (source.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException();
        }

        OffsetDateTime timestamp = OffsetDateTime.now();
        Transaction transaction = transactionRepository.save(Transaction.builder()
                .customerId(customerId)
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.PENDING_EVALUATION)
                .amount(amount)
                .sourceAccountId(sourceId)
                .destinationAccountId(destinationId)
                .description(normalizeDescription(request.description()))
                .timestamp(timestamp)
                .build());

        FraudEvaluationResult fraudResult = fraudEvaluationPort.evaluate(new FraudEvaluationRequestedEvent(
                transaction.getId(),
                customerId,
                amount,
                sourceId,
                destinationId,
                timestamp
        ));

        if (fraudResult.blocked()) {
            transaction.setStatus(TransactionStatus.BLOCKED);
            Transaction saved = transactionRepository.save(transaction);
            log.info("Transfer blocked: transactionId={}, customerId={}", saved.getId(), customerId);
            return new TransferResponse(saved.getId(), saved.getStatus());
        }

        source.setBalance(source.getBalance().subtract(amount));
        destination.setBalance(destination.getBalance().add(amount));
        transaction.setStatus(TransactionStatus.COMPLETED);

        Transaction saved = transactionRepository.save(transaction);
        log.info("Transfer completed: transactionId={}, customerId={}, amount={}",
                saved.getId(), customerId, amount);
        return new TransferResponse(saved.getId(), saved.getStatus());
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }
        String trimmed = description.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private TransactionSummaryResponse toSummary(Transaction transaction) {
        return new TransactionSummaryResponse(
                transaction.getId(),
                transaction.getCustomerId(),
                transaction.getType(),
                transaction.getStatus(),
                transaction.getAmount(),
                transaction.getSourceAccountId(),
                transaction.getDestinationAccountId(),
                transaction.getDescription(),
                transaction.getTimestamp()
        );
    }

    private TransactionDetailResponse toDetail(Transaction transaction) {
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
