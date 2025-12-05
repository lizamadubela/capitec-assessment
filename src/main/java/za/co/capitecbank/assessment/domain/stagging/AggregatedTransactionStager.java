package za.co.capitecbank.assessment.domain.stagging;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import za.co.capitecbank.assessment.domain.entity.AggregatedTransaction;
import za.co.capitecbank.assessment.domain.entity.RawTransaction;
import za.co.capitecbank.assessment.repository.AggregatedTransactionRepository;
import za.co.capitecbank.assessment.service.TxCategorizationEngine;
import za.co.capitecbank.assessment.tx_source.TransactionSource;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class AggregatedTransactionStager {

    private final List<TransactionSource> sources;
    private final TxCategorizationEngine engine;
    private final AggregatedTransactionRepository aggregatedTransactionRepository;
    public AggregatedTransactionStager(List<TransactionSource> sources,
                                  TxCategorizationEngine engine, AggregatedTransactionRepository aggregatedTransactionRepository) {
        this.sources = sources;
        this.engine = engine;
        this.aggregatedTransactionRepository = aggregatedTransactionRepository;
    }
    @PostConstruct
    private void stage() {
        sources.forEach(transactionSource -> {
            List<RawTransaction> rawTransactions = transactionSource.fetchTransactions();
            List<AggregatedTransaction> entities = rawTransactions.stream()
                    .map(rawTransaction -> engine.categorize(rawTransaction))
                    .map(aggregatedTransaction -> new AggregatedTransaction(aggregatedTransaction.getId(),aggregatedTransaction.getCustomerId(),
                            aggregatedTransaction.getAmount(), aggregatedTransaction.getTimestamp(), aggregatedTransaction.getDescription(), aggregatedTransaction.getCategory(),
                            aggregatedTransaction.getSource()))
                    .collect(Collectors.toList());
            aggregatedTransactionRepository.saveAll(entities);
        });
    }
}
