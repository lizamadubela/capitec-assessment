package za.co.capitecbank.assessment.tx_source;

import za.co.capitecbank.assessment.domain.RawTransaction;

import java.util.List;

public interface TransactionSource {
    List<RawTransaction> fetchTransactions(String customerId);
}
