package za.co.capitecbank.assessment.tx_source.impl;

import org.springframework.stereotype.Component;
import za.co.capitecbank.assessment.domain.entity.RawTransaction;
import za.co.capitecbank.assessment.tx_source.TransactionSource;

import java.util.List;

@Component
public class SnapScan implements TransactionSource {
    @Override
    public List<RawTransaction> fetchTransactions() {
        return List.of();
    }
}
