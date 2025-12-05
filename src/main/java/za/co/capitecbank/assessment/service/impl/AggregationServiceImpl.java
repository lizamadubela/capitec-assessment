package za.co.capitecbank.assessment.service.impl;

import org.springframework.stereotype.Service;
import za.co.capitecbank.assessment.domain.entity.AggregatedTransaction;
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
        return aggregatedTransactionRepository.findByCustomerIdOrderByTimestampDesc(customerId);
    }

    @Override
    public Map<String, BigDecimal> getTotalsByCategory(String customerId) {
        Map<String, BigDecimal> totals = new HashMap<String, BigDecimal>();

        for (AggregatedTransaction a :
                aggregatedTransactionRepository.findByCustomerIdOrderByTimestampDesc(customerId)) {

            String category = a.getCategory();
            if (category == null) {
                continue;
            }

            BigDecimal amount = a.getAmount();
            BigDecimal current = totals.get(category);

            totals.put(category, current == null ? amount : current.add(amount));
        }

        return totals;
    }

    @Override
    public List<AggregatedTransaction> getByDateRange(String customerId, LocalDate
            start, LocalDate end) {
        LocalDateTime startDt = start.atStartOfDay();
        LocalDateTime endDt = end.atTime(23, 59, 59);

        return aggregatedTransactionRepository.findByCustomerIdAndTimestampBetweenOrderByTimestampDesc(customerId,
                        startDt, endDt);
    }

    @Override
    public AggregatedTransaction getTransactionById(String customerId,Long transactionId) {
        return aggregatedTransactionRepository.findByCustomerIdAndId(customerId,transactionId).orElseThrow(() -> new TransactionNotFoundException(transactionId,customerId)); // todo return a specific message when not foud
    }

    @Override
    public Map<String, BigDecimal> getTotalsBySource(String customerId) {
        return Map.of();
    }

    @Override
    public List<AggregatedTransaction> searchTransactions(String search, String customerId) {
        return List.of();
    }
}