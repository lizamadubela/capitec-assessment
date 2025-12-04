package za.co.capitecbank.assessment.service.impl;

import org.springframework.stereotype.Service;
import za.co.capitecbank.assessment.domain.entity.AggregatedTransaction;
import za.co.capitecbank.assessment.domain.entity.RawTransaction;
import za.co.capitecbank.assessment.repository.RawTransactionRepository;
import za.co.capitecbank.assessment.repository.TransactionRepository;
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
    private final TransactionRepository repository;
    private final RawTransactionRepository rawTransactionRepository;

    public AggregationServiceImpl(List<TransactionSource> sources,
                                  TxCategorizationEngine engine, TransactionRepository repository, RawTransactionRepository rawTransactionRepository) {
        this.sources = sources;
        this.engine = engine;
        this.repository = repository;
        this.rawTransactionRepository = rawTransactionRepository;
    }

    @Override
    public List<za.co.capitecbank.assessment.domain.Transaction> getAllTransactions(String customerId) {
// fetch and save
        sources.forEach(transactionSource -> {
            List<RawTransaction> rawTransactions = transactionSource.fetchTransactions();
            List<AggregatedTransaction> entities = rawTransactions.stream()
                    .map(r -> engine.categorize(r,
                            transactionSource.getClass().getSimpleName()))
                    .map(t -> new AggregatedTransaction(t.getCustomerId(),
                            t.getAmount(), t.getTimestamp(), t.getDescription(), t.getCategory(),
                            t.getSource()))
                    .collect(Collectors.toList());
            repository.saveAll(entities);
        });
// read back from DB
        return
                repository.findByCustomerIdOrderByTimestampDesc(customerId).stream()
                        .map(e -> new za.co.capitecbank.assessment.domain.Transaction(UUID.randomUUID().toString(), e.getCustomerId(), e.getAmount(),
                                e.getTimestamp(), e.getDescription(), e.getCategory(), e.getSource()))
                        .sorted(Comparator.comparing(za.co.capitecbank.assessment.domain.Transaction::getTimestamp).reversed())
                        .toList();
    }
    @Override
    public Map<String, BigDecimal> getTotalsByCategory(String customerId) {
        return
                repository.findByCustomerIdOrderByTimestampDesc(customerId).stream()
                        .map(e -> new za.co.capitecbank.assessment.domain.Transaction(UUID.randomUUID().toString(), e.getCustomerId(), e.getAmount(),
                                e.getTimestamp(), e.getDescription(), e.getCategory(), e.getSource()))
                        .collect(Collectors.groupingBy(za.co.capitecbank.assessment.domain.Transaction::getCategory,
                                Collectors.mapping(za.co.capitecbank.assessment.domain.Transaction::getAmount,
                                        Collectors.reducing(BigDecimal.ZERO,
                                                BigDecimal::add))));
    }
    @Override
    public List<za.co.capitecbank.assessment.domain.Transaction> getByDateRange(String customerId, LocalDate
            start, LocalDate end) {
        LocalDateTime startDt = start.atStartOfDay();
        LocalDateTime endDt = end.atTime(23,59,59);

        return
                repository.findByCustomerIdAndTimestampBetweenOrderByTimestampDesc(customerId,
                                startDt, endDt).stream()
                        .map(e -> new za.co.capitecbank.assessment.domain.Transaction(UUID.randomUUID().toString(),e.getCustomerId(), e.getAmount(),
                                e.getTimestamp(), e.getDescription(), e.getCategory(), e.getSource()))
                        .sorted(Comparator.comparing(za.co.capitecbank.assessment.domain.Transaction::getTimestamp).reversed())
                        .toList();
    }
}