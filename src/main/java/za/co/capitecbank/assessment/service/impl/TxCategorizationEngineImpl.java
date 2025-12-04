package za.co.capitecbank.assessment.service.impl;

import org.springframework.stereotype.Service;
import za.co.capitecbank.assessment.domain.entity.AggregatedTransaction;
import za.co.capitecbank.assessment.domain.entity.RawTransaction;
import za.co.capitecbank.assessment.domain.entity.TransactionCategory;
import za.co.capitecbank.assessment.service.loader.CategoryDataLoaderService;
import za.co.capitecbank.assessment.service.TxCategorizationEngine;

import java.util.UUID;

@Service
public class TxCategorizationEngineImpl implements TxCategorizationEngine {

    private final CategoryDataLoaderService categoryService;

    public TxCategorizationEngineImpl(CategoryDataLoaderService categoryService) {
        this.categoryService = categoryService;
    }

    public AggregatedTransaction categorize(RawTransaction raw, String source) {
        TransactionCategory category = categoryService.categorize(
                raw.getDescription(),
                raw.getAmount()
        );

        return new AggregatedTransaction(
                raw.getId(),
                raw.getCustomerId(),
                raw.getAmount(),
                raw.getTimestamp(),
                raw.getDescription(),
                category.getDisplayName(),
                raw.getSource()
        );
    }
}

