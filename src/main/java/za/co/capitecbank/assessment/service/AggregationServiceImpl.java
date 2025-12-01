package za.co.capitecbank.assessment.service;

import org.springframework.stereotype.Service;
import za.co.capitecbank.assessment.domain.RawTransaction;
import za.co.capitecbank.assessment.domain.entity.Transaction;
import za.co.capitecbank.assessment.repository.TransactionRepository;
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
    private final List<TransactionSource> providers;
    private final TxCategorizationEngine engine;
    private final TransactionRepository repository;

    public AggregationServiceImpl(List<TransactionSource> providers,
                                  TxCategorizationEngine engine, TransactionRepository repository) {
        this.providers = providers;
        this.engine = engine;
        this.repository = repository;
    }

    @Override
    public List<za.co.capitecbank.assessment.domain.Transaction> getAllTransactions(String customerId) {
// fetch and save
        providers.forEach(p -> {
            List<RawTransaction> raws = p.fetchTransactions(customerId);
            List<Transaction> entities = raws.stream()
                    .map(r -> engine.categorize(r,
                            p.getClass().getSimpleName()))
                    .map(t -> new Transaction(t.getCustomerId(),
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