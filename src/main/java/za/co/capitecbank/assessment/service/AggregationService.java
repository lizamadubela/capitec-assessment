package za.co.capitecbank.assessment.service;
import za.co.capitecbank.assessment.domain.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;


public interface AggregationService {
    List<Transaction> getAllTransactions(String customerId);
    Map<String, BigDecimal> getTotalsByCategory(String customerId);
    List<Transaction> getByDateRange(String customerId, LocalDate start, LocalDate end);
}
