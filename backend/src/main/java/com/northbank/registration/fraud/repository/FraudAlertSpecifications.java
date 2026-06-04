// Story: US-017
package com.northbank.registration.fraud.repository;

import com.northbank.registration.fraud.domain.model.FraudAlert;
import com.northbank.registration.fraud.domain.model.FraudAlertReviewStatus;
import com.northbank.registration.fraud.domain.model.FraudRuleConditionType;
import com.northbank.registration.fraud.service.dto.FraudAlertFilter;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;

public final class FraudAlertSpecifications {

    private FraudAlertSpecifications() {
    }

    public static Specification<FraudAlert> matches(FraudAlertFilter filter) {
        FraudAlertFilter safeFilter = filter == null
                ? new FraudAlertFilter(null, null, null, null)
                : filter;
        return Specification
                .where(from(safeFilter.dateFrom()))
                .and(to(safeFilter.dateTo()))
                .and(reviewStatus(safeFilter.reviewStatus()))
                .and(ruleConditionType(safeFilter.ruleConditionType()));
    }

    private static Specification<FraudAlert> from(OffsetDateTime from) {
        return (root, query, cb) -> from == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("timestamp"), from);
    }

    private static Specification<FraudAlert> to(OffsetDateTime to) {
        return (root, query, cb) -> to == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("timestamp"), to);
    }

    private static Specification<FraudAlert> reviewStatus(FraudAlertReviewStatus reviewStatus) {
        return (root, query, cb) -> reviewStatus == null ? cb.conjunction() : cb.equal(root.get("reviewStatus"), reviewStatus);
    }

    private static Specification<FraudAlert> ruleConditionType(FraudRuleConditionType ruleConditionType) {
        return (root, query, cb) -> ruleConditionType == null ? cb.conjunction() : cb.equal(root.get("ruleConditionType"), ruleConditionType);
    }
}
