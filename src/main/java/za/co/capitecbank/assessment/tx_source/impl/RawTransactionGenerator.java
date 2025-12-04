package za.co.capitecbank.assessment.tx_source.impl;

import za.co.capitecbank.assessment.domain.entity.RawTransaction;
import za.co.capitecbank.assessment.tx_source.TransactionSource;

import java.util.List;

public class RawTransactionGenerator implements TransactionSource {
    @Override
    public List<RawTransaction> fetchTransactions() {
        //TODO read the local categories a create random transactions from it.
        return List.of();
    }
}
