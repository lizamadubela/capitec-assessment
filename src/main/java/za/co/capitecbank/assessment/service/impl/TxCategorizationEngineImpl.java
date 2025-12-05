package za.co.capitecbank.assessment.service.impl;

import org.springframework.stereotype.Service;
import za.co.capitecbank.assessment.domain.entity.AggregatedTransaction;
import za.co.capitecbank.assessment.domain.entity.RawTransaction;
import za.co.capitecbank.assessment.domain.entity.TransactionCategory;
import za.co.capitecbank.assessment.service.loader.CategoryDataLoaderService;
import za.co.capitecbank.assessment.service.TxCategorizationEngine;


@Service
public class TxCategorizationEngineImpl implements TxCategorizationEngine {

    private final CategoryDataLoaderService categoryService;

    public TxCategorizationEngineImpl(CategoryDataLoaderService categoryService) {
        this.categoryService = categoryService;
    }

    public AggregatedTransaction categorize(RawTransaction rawTransaction) {
        TransactionCategory category = categoryService.categorize(
                rawTransaction.getDescription(),
                rawTransaction.getAmount()
        );

        return new AggregatedTransaction(
                rawTransaction.getId(),
                rawTransaction.getCustomerId(),
                rawTransaction.getAmount(),
                rawTransaction.getTimestamp(),
                rawTransaction.getDescription(),
                category.getDisplayName(),
                rawTransaction.getSource()
        );
    }
}

