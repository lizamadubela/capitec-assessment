package za.co.capitecbank.assessment.transactions.source;

import za.co.capitecbank.assessment.domain.entity.RawTransaction;

import java.util.List;

public interface TransactionSource {
    List<RawTransaction> fetchTransactions();
}
