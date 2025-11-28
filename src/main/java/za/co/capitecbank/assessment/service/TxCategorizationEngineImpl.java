package za.co.capitecbank.assessment.service;

import org.springframework.stereotype.Service;
import za.co.capitecbank.assessment.domain.RawTransaction;
import za.co.capitecbank.assessment.domain.Transaction;
import za.co.capitecbank.assessment.domain.entity.TransactionCategory;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class TxCategorizationEngineImpl implements TxCategorizationEngine {

    private final CategoryService categoryService;

    public TxCategorizationEngineImpl(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    public Transaction categorize(RawTransaction raw, String source) {
        TransactionCategory category = categoryService.categorize(
                raw.getDescription(),
                raw.getAmount()
        );

        return new Transaction(
                UUID.randomUUID().toString(),
                raw.getCustomerId(),
                raw.getAmount(),
                raw.getTimestamp(),
                raw.getDescription(),
                category.getDisplayName(),
                source
        );
    }
}

