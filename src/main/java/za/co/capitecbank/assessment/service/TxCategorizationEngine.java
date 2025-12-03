package za.co.capitecbank.assessment.service;


import za.co.capitecbank.assessment.domain.entity.RawTransaction;
import za.co.capitecbank.assessment.domain.Transaction;

public interface TxCategorizationEngine {
    Transaction categorize(RawTransaction raw, String source);
}
