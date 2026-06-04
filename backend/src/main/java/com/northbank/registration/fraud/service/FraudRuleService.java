// Story: US-014
package com.northbank.registration.fraud.service;

import com.northbank.registration.fraud.domain.model.FraudRule;
import com.northbank.registration.fraud.domain.model.FraudRuleConditionType;
import com.northbank.registration.fraud.domain.model.FraudRuleStatus;
import com.northbank.registration.fraud.exception.DuplicateFraudRuleNameException;
import com.northbank.registration.fraud.exception.FraudRuleNotFoundException;
import com.northbank.registration.fraud.exception.InvalidFraudRuleThresholdException;
import com.northbank.registration.fraud.exception.LastActiveFraudRuleException;
import com.northbank.registration.fraud.repository.FraudRuleRepository;
import com.northbank.registration.fraud.service.dto.CreateFraudRuleRequest;
import com.northbank.registration.fraud.service.dto.FraudRuleResponse;
import com.northbank.registration.fraud.service.dto.UpdateFraudRuleRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FraudRuleService {

    private final FraudRuleRepository fraudRuleRepository;

    @Transactional
    public FraudRuleResponse create(CreateFraudRuleRequest request) {
        String name = normalizeName(request.name());
        if (fraudRuleRepository.existsByName(name)) {
            throw new DuplicateFraudRuleNameException();
        }
        validateThreshold(request.conditionType(), request.thresholdValue());

        FraudRule rule = fraudRuleRepository.save(FraudRule.builder()
                .name(name)
                .conditionType(request.conditionType())
                .thresholdValue(request.thresholdValue().trim())
                .active(request.active())
                .status(FraudRuleStatus.ACTIVE)
                .build());
        return toResponse(rule);
    }

    @Transactional(readOnly = true)
    public Page<FraudRuleResponse> list(Pageable pageable) {
        return fraudRuleRepository.findAllByStatusNot(FraudRuleStatus.DELETED, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public FraudRuleResponse update(UUID id, UpdateFraudRuleRequest request) {
        FraudRule rule = fraudRuleRepository.findById(id)
                .filter(existing -> existing.getStatus() != FraudRuleStatus.DELETED)
                .orElseThrow(FraudRuleNotFoundException::new);

        if (request.name() != null) {
            String name = normalizeName(request.name());
            if (fraudRuleRepository.existsByNameAndIdNot(name, id)) {
                throw new DuplicateFraudRuleNameException();
            }
            rule.setName(name);
        }
        if (request.thresholdValue() != null) {
            validateThreshold(rule.getConditionType(), request.thresholdValue());
            rule.setThresholdValue(request.thresholdValue().trim());
        }
        if (request.active() != null) {
            rule.setActive(request.active());
        }
        return toResponse(fraudRuleRepository.save(rule));
    }

    @Transactional
    public void delete(UUID id) {
        FraudRule rule = fraudRuleRepository.findById(id)
                .filter(existing -> existing.getStatus() != FraudRuleStatus.DELETED)
                .orElseThrow(FraudRuleNotFoundException::new);

        if (rule.isActive() && fraudRuleRepository.countByActiveTrueAndStatus(FraudRuleStatus.ACTIVE) <= 1) {
            throw new LastActiveFraudRuleException();
        }

        rule.setActive(false);
        rule.setStatus(FraudRuleStatus.DELETED);
        fraudRuleRepository.save(rule);
    }

    private String normalizeName(String name) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isBlank()) {
            throw new InvalidFraudRuleThresholdException("Rule name is required");
        }
        return normalized;
    }

    public void validateThreshold(FraudRuleConditionType conditionType, String thresholdValue) {
        if (thresholdValue == null || thresholdValue.isBlank()) {
            throw new InvalidFraudRuleThresholdException("Threshold value is required");
        }
        String trimmed = thresholdValue.trim();
        try {
            switch (conditionType) {
                case AMOUNT_EXCEEDS -> {
                    if (new BigDecimal(trimmed).compareTo(BigDecimal.ZERO) <= 0) {
                        throw new InvalidFraudRuleThresholdException("Amount threshold must be greater than zero");
                    }
                }
                case TRANSACTION_FREQUENCY -> {
                    int threshold = Integer.parseInt(trimmed);
                    if (threshold <= 0) {
                        throw new InvalidFraudRuleThresholdException("Frequency threshold must be greater than zero");
                    }
                }
                case UNUSUAL_HOUR -> parseHourRange(trimmed);
            }
        } catch (NumberFormatException ex) {
            throw new InvalidFraudRuleThresholdException("Threshold value is invalid");
        }
    }

    public HourRange parseHourRange(String thresholdValue) {
        String[] parts = thresholdValue.split("-");
        if (parts.length != 2) {
            throw new InvalidFraudRuleThresholdException("Hour threshold must use HH:mm-HH:mm format");
        }
        try {
            LocalTime start = LocalTime.parse(parts[0].trim());
            LocalTime end = LocalTime.parse(parts[1].trim());
            if (start.equals(end)) {
                throw new InvalidFraudRuleThresholdException("Hour range start and end must differ");
            }
            return new HourRange(start, end);
        } catch (RuntimeException ex) {
            if (ex instanceof InvalidFraudRuleThresholdException thresholdException) {
                throw thresholdException;
            }
            throw new InvalidFraudRuleThresholdException("Hour threshold must use HH:mm-HH:mm format");
        }
    }

    private FraudRuleResponse toResponse(FraudRule rule) {
        return new FraudRuleResponse(
                rule.getId(),
                rule.getName(),
                rule.getConditionType(),
                rule.getThresholdValue(),
                rule.isActive(),
                rule.getStatus(),
                rule.getCreatedAt(),
                rule.getUpdatedAt()
        );
    }

    public record HourRange(LocalTime start, LocalTime end) {
    }
}
