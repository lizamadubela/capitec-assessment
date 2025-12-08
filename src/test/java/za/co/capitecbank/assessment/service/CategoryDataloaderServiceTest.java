package za.co.capitecbank.assessment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.capitecbank.assessment.domain.entity.AggregatedTransaction;
import za.co.capitecbank.assessment.domain.entity.CategoryKeyword;
import za.co.capitecbank.assessment.domain.entity.RawTransaction;
import za.co.capitecbank.assessment.domain.entity.TransactionCategory;
import za.co.capitecbank.assessment.service.impl.TxCategorizationEngineImpl;
import za.co.capitecbank.assessment.service.loader.CategoryDataLoaderService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TxCategorizationEngine Implementation Tests")
class TxCategorizationEngineImplTest {

    @Mock
    private CategoryDataLoaderService categoryService;

    @InjectMocks
    private TxCategorizationEngineImpl categorizationEngine;

    private RawTransaction sampleRawTransaction;
    private TransactionCategory sampleCategory;

    @BeforeEach
    void setUp() {
        sampleRawTransaction= new RawTransaction(
                "CUST-1",
                "purchase at nandos",
                new BigDecimal("50.00"),
                LocalDateTime.now(),
                "WEB"
        );

        sampleCategory = createCategory(
                "FOOD",
                "Food",
                List.of(
                        new CategoryKeyword("nandos"),
                        new CategoryKeyword("kfc")
                )
        );
    }

    @Test
    void shouldCategorizeTransaction() {
        when(categoryService.categorize(
                sampleRawTransaction.getDescription(),
                sampleRawTransaction.getAmount()
        )).thenReturn(sampleCategory);

        AggregatedTransaction result = categorizationEngine.categorize(sampleRawTransaction);
        assertThat(result.getCategory()).isEqualTo("Food");
        assertThat(result.getDescription()).contains("nandos");
    }
    private TransactionCategory createCategory(String categoryName, String displayName,List<CategoryKeyword> keywords) {
        TransactionCategory category = new TransactionCategory();
        category.setName(categoryName);
        category.setDisplayName(displayName);
        category.setKeywords(keywords);
        return category;
    }
}