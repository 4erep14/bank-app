// Story: US-010
package com.northbank.registration.transaction.service;

import com.northbank.registration.transaction.domain.event.FraudEvaluationRequestedEvent;
import org.springframework.stereotype.Service;

/**
 * Default fraud decision adapter until US-015 supplies active fraud rules.
 */
@Service
public class AllowAllFraudEvaluationService implements FraudEvaluationPort {
    @Override
    public FraudEvaluationResult evaluate(FraudEvaluationRequestedEvent event) {
        return FraudEvaluationResult.allowed();
    }
}
