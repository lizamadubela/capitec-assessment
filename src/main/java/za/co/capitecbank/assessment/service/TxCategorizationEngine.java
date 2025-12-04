package za.co.capitecbank.assessment.service;

import za.co.capitecbank.assessment.domain.entity.AggregatedTransaction;
import za.co.capitecbank.assessment.domain.entity.RawTransaction;

public interface TxCategorizationEngine {
    AggregatedTransaction categorize(RawTransaction raw, String source);
}