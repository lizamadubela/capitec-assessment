package za.co.capitecbank.assessment.service;

import za.co.capitecbank.assessment.domain.entity.AggregatedTransaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;



public interface AggregationService {
    List<AggregatedTransaction> getAllTransactions(String customerId);
    Map<String, BigDecimal> getTotalsByCategory(String customerId);
    List<AggregatedTransaction> getByDateRange(String customerId, LocalDate start, LocalDate end);
    AggregatedTransaction getTransactionById(String customerId, Long transactionId);
    Map<String, BigDecimal> getTotalsBySource(String customerId);
//    List<AggregatedTransaction> searchTransactions(String search, String customerId);
}
