package za.co.capitecbank.assessment.service.impl;

import org.springframework.stereotype.Service;
import za.co.capitecbank.assessment.domain.entity.AggregatedTransaction;
import za.co.capitecbank.assessment.exception.CustomerNotFoundException;
import za.co.capitecbank.assessment.exception.TransactionNotFoundException;
import za.co.capitecbank.assessment.repository.AggregatedTransactionRepository;
import za.co.capitecbank.assessment.service.AggregationService;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;


@Service
public class AggregationServiceImpl implements AggregationService {
    private final AggregatedTransactionRepository aggregatedTransactionRepository;

    public AggregationServiceImpl(AggregatedTransactionRepository aggregatedTransactionRepository) {
        this.aggregatedTransactionRepository = aggregatedTransactionRepository;
    }

    @Override
    public List<AggregatedTransaction> getAllTransactions(String customerId) {
        List<AggregatedTransaction> aggregatedTransactions = aggregatedTransactionRepository.findByCustomerIdOrderByTimestampDesc(customerId);
        if (aggregatedTransactions.isEmpty()) {
            throw new CustomerNotFoundException(customerId);
        }
        return aggregatedTransactions;
    }

    @Override
    public Map<String, BigDecimal> getTotalsByCategory(String customerId) {
        Map<String, BigDecimal> totals = new HashMap<String, BigDecimal>();
        List<AggregatedTransaction> aggregatedTransactions = aggregatedTransactionRepository.findByCustomerIdOrderByTimestampDesc(customerId);
        if (aggregatedTransactions.isEmpty()) {
            throw new CustomerNotFoundException(customerId + " therefore cannot calculate the totals");
        }
        for (AggregatedTransaction transaction :
                aggregatedTransactions) {

            String category = transaction.getCategory();
            if (category == null) {
                continue;
            }

            // I'm treating amount as positive (CREDIT vs DEBIT)
            BigDecimal amount = transaction.getAmount().abs();
            totals.merge(category, amount, BigDecimal::add);
        }

        return totals;
    }

    @Override
    public List<AggregatedTransaction> getByDateRange(String customerId, LocalDate
            start, LocalDate end) {
        LocalDateTime startDt = start.atStartOfDay();
        LocalDateTime endDt = end.atTime(23, 59, 59);

        List<AggregatedTransaction> aggregatedTransactions = aggregatedTransactionRepository.findByCustomerIdAndTimestampBetweenOrderByTimestampDesc(customerId,
                startDt, endDt);
        if (aggregatedTransactions.isEmpty()) {
            throw new RuntimeException("no transactions found for this date range");
        }

        return aggregatedTransactionRepository.findByCustomerIdAndTimestampBetweenOrderByTimestampDesc(customerId,
                startDt, endDt);
    }

    @Override
    public AggregatedTransaction getTransactionById(String customerId, Long transactionId) {
        return aggregatedTransactionRepository.findByCustomerIdAndId(customerId, transactionId).orElseThrow(() -> new TransactionNotFoundException(transactionId, customerId));
    }

    @Override
    public Map<String, BigDecimal> getTotalsBySource(String customerId) {
        Map<String, BigDecimal> totals = new HashMap<String, BigDecimal>();

        for (AggregatedTransaction aggregatedTransaction :
                aggregatedTransactionRepository.findByCustomerIdOrderByTimestampDesc(customerId)) {

            String source = aggregatedTransaction.getSource();
            if (source == null) {
                continue;
            }

            BigDecimal amount = aggregatedTransaction.getAmount();
            BigDecimal current = totals.get(source);

            totals.put(source, current == null ? amount : current.add(amount));
        }
        return totals;
    }
}