package za.co.capitecbank.assessment.service.impl;

import org.springframework.stereotype.Service;
import za.co.capitecbank.assessment.domain.entity.AggregatedTransaction;
import za.co.capitecbank.assessment.domain.entity.RawTransaction;
import za.co.capitecbank.assessment.repository.RawTransactionRepository;
import za.co.capitecbank.assessment.repository.AggregatedTransactionRepository;
import za.co.capitecbank.assessment.service.AggregationService;
import za.co.capitecbank.assessment.service.TxCategorizationEngine;
import za.co.capitecbank.assessment.tx_source.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AggregationServiceImpl implements AggregationService {
    private final List<TransactionSource> sources;
    private final TxCategorizationEngine engine;
    private final AggregatedTransactionRepository aggregatedTransactionRepository;
    private final RawTransactionRepository rawTransactionRepository;

    public AggregationServiceImpl(List<TransactionSource> sources,
                                  TxCategorizationEngine engine, AggregatedTransactionRepository aggregatedTransactionRepository, RawTransactionRepository rawTransactionRepository) {
        this.sources = sources;
        this.engine = engine;
        this.aggregatedTransactionRepository = aggregatedTransactionRepository;
        this.rawTransactionRepository = rawTransactionRepository;
    }

    @Override
    public List<AggregatedTransaction> getAllTransactions(String customerId) {
        return  aggregatedTransactionRepository.findByCustomerIdOrderByTimestampDesc(customerId);
    }
    @Override
    public Map<String, BigDecimal> getTotalsByCategory(String customerId) {
        return
                aggregatedTransactionRepository.findByCustomerIdOrderByTimestampDesc(customerId).stream()
                        .map(e -> new za.co.capitecbank.assessment.domain.Transaction(UUID.randomUUID().toString(), e.getCustomerId(), e.getAmount(),
                                e.getTimestamp(), e.getDescription(), e.getCategory(), e.getSource()))
                        .collect(Collectors.groupingBy(za.co.capitecbank.assessment.domain.Transaction::getCategory,
                                Collectors.mapping(za.co.capitecbank.assessment.domain.Transaction::getAmount,
                                        Collectors.reducing(BigDecimal.ZERO,
                                                BigDecimal::add))));
    }
    @Override
    public List<AggregatedTransaction> getByDateRange(String customerId, LocalDate
            start, LocalDate end) {
        LocalDateTime startDt = start.atStartOfDay();
        LocalDateTime endDt = end.atTime(23,59,59);

        return
                aggregatedTransactionRepository.findByCustomerIdAndTimestampBetweenOrderByTimestampDesc(customerId,
                                startDt, endDt);
    }
}