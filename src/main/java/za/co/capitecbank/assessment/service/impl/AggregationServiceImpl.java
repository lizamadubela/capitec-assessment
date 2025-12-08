package za.co.capitecbank.assessment.service.impl;

import org.springframework.stereotype.Service;
import za.co.capitecbank.assessment.domain.entity.AggregatedTransaction;
import za.co.capitecbank.assessment.exception.CustomerNotFoundException;
import za.co.capitecbank.assessment.exception.InvalidDateRangeException;
import za.co.capitecbank.assessment.exception.InvalidSearchQueryException;
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
        isValidCustomer(customerId);
        return aggregatedTransactionRepository.findByCustomerIdOrderByTimestampDesc(customerId);
    }

    @Override
    public Map<String, BigDecimal> getTotalsByCategory(String customerId) {
        isValidCustomer(customerId);
        Map<String, BigDecimal> totals = new HashMap<String, BigDecimal>();
        List<AggregatedTransaction> aggregatedTransactions = aggregatedTransactionRepository.findByCustomerIdOrderByTimestampDesc(customerId);
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
    public List<AggregatedTransaction> getByDateRange(String customerId, LocalDate start, LocalDate end) {
        isValidCustomer(customerId);
        if (start == null || end == null) {
            throw new InvalidDateRangeException("Start date and end date are required");
        }
        if (start.isAfter(end)) {
            throw new InvalidDateRangeException("Start date cannot be after end date");
        }
        LocalDateTime startDt = start.atStartOfDay();
        LocalDateTime endDt = end.atTime(23, 59, 59);
        return aggregatedTransactionRepository.findByCustomerIdAndTimestampBetweenOrderByTimestampDesc(
                customerId, startDt, endDt);
    }

    @Override
    public AggregatedTransaction getTransactionById(String customerId, Long transactionId) {
        isValidCustomer(customerId);
        return aggregatedTransactionRepository.findByCustomerIdAndId(customerId, transactionId).orElseThrow(() -> new TransactionNotFoundException(transactionId, customerId));
    }

    @Override
    public Map<String, BigDecimal> getTotalsBySource(String customerId) {
        isValidCustomer(customerId);
        Map<String, BigDecimal> totals = new HashMap<String, BigDecimal>();

        for (AggregatedTransaction transaction :
                aggregatedTransactionRepository.findByCustomerIdOrderByTimestampDesc(customerId)) {

            String source = transaction.getSource();
            if (source == null) {
                continue;
            }

            // I'm treating amount as positive (CREDIT vs DEBIT)
            BigDecimal amount = transaction.getAmount().abs();
            totals.merge(source, amount, BigDecimal::add);
        }
        return totals;
    }

    @Override
    public List<AggregatedTransaction> searchTransactions(String customerId, String search) {
        isValidCustomer(customerId);

        if (search == null || search.trim().isEmpty()) {
            throw new InvalidSearchQueryException("Search query cannot be empty");
        }

        String normalizedSearch = search.trim().toLowerCase();

        // Minimum search length to prevent too broad searches
        if (normalizedSearch.length() < 2) {
            throw new InvalidSearchQueryException("Search query must be at least 2 characters");
        }

        return aggregatedTransactionRepository.searchTransactions(customerId, normalizedSearch);
    }

    private void isValidCustomer(String customerId) {
        if (!aggregatedTransactionRepository.existsAggregatedTransactionByCustomerId(customerId)) {
            throw new CustomerNotFoundException("Customer " + customerId +" not found");
        }
    }
}