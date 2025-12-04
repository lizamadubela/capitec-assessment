package za.co.capitecbank.assessment.tx_source.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import za.co.capitecbank.assessment.domain.entity.RawTransaction;
import za.co.capitecbank.assessment.repository.RawTransactionRepository;
import za.co.capitecbank.assessment.tx_source.TransactionSource;

import java.util.ArrayList;
import java.util.List;

@Component
public class Bank implements TransactionSource {
    @Autowired
    RawTransactionRepository transactionRepository;
    @Override
    public List<RawTransaction> fetchTransactions() {
        return new ArrayList<>(transactionRepository.findAll());
    }
}
