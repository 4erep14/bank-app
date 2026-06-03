// Story: US-010
package com.northbank.registration.transaction.service;

import com.northbank.registration.transaction.domain.event.FraudEvaluationRequestedEvent;

public interface FraudEvaluationPort {
    FraudEvaluationResult evaluate(FraudEvaluationRequestedEvent event);
}
