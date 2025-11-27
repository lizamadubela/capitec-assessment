package za.co.capitecbank.assessment.service;

import org.springframework.stereotype.Service;
import za.co.capitecbank.assessment.domain.Transaction;
import za.co.capitecbank.assessment.tx_source.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Service
public class AggregationServiceImpl implements AggregationService {

    private final List<TransactionSource> txSources;
    private final TxCategorizationEngine engine;

    public AggregationServiceImpl(List<TransactionSource> txSources,
                                  TxCategorizationEngine engine) {
        this.txSources = txSources;
        this.engine = engine;
    }
    @Override
    public List<Transaction> getAllTransactions(String customerId) {
        return txSources.stream()
                .flatMap(p -> p.fetchTransactions(customerId).stream()
                        .map(raw -> engine.categorize(raw,
                                p.getClass().getSimpleName())))
                .sorted(Comparator.comparing(Transaction::getTimestamp).reversed())
                .toList();
    }
    @Override
    public Map<String, BigDecimal> getTotalsByCategory(String customerId) {
        return getAllTransactions(customerId).stream()
                .collect(Collectors.groupingBy(Transaction::getCategory,
                        Collectors.mapping(Transaction::getAmount,
                                Collectors.reducing(BigDecimal.ZERO,
                                        BigDecimal::add))));
    }
    @Override
    public List<Transaction> getByDateRange(String customerId, LocalDate
            start, LocalDate end) {
        return getAllTransactions(customerId).stream()
                .filter(t -> {
                    var date = t.getTimestamp().toLocalDate();
                    return !(date.isBefore(start) || date.isAfter(end));
                })
                .toList();
    }
}