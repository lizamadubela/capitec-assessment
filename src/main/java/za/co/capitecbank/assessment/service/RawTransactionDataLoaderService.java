package za.co.capitecbank.assessment.service;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import za.co.capitecbank.assessment.domain.entity.RawTransaction;
import za.co.capitecbank.assessment.repository.RawTransactionRepository;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


@Component
@Slf4j
@Order(2)
public class RawTransactionDataLoaderService {

    private final RawTransactionRepository rawTransactionRepository;
    private final ResourceLoader resourceLoader;
    private final TxCategorizationEngine categorizationEngine;
    private final CsvMapper csvMapper;

    @Value("${app.data-file:classpath:transactions.csv}")
    private String transactionsFile;

    public RawTransactionDataLoaderService(RawTransactionRepository rawTransactionRepository,
                                           ResourceLoader resourceLoader,
                                           TxCategorizationEngine categorizationEngine) {
        this.rawTransactionRepository = rawTransactionRepository;
        this.resourceLoader = resourceLoader;
        this.categorizationEngine = categorizationEngine;
        this.csvMapper = new CsvMapper();
        this.csvMapper.registerModule(new JavaTimeModule());
    }

    @PostConstruct
    public void loadTransactionsFromCsv() {
        try {
            Resource resource = resourceLoader.getResource(transactionsFile);

            if (!resource.exists()) {
                log.warn("CSV file not found at {}, skipping transaction load", transactionsFile);
                return;
            }

            log.info("Loading transactions from CSV: {}", transactionsFile);

            CsvSchema schema = CsvSchema.emptySchema()
                    .withHeader()
                    .withColumnSeparator(',')
                    .withQuoteChar('"');

            try (InputStream is = resource.getInputStream()) {
                MappingIterator<TransactionCsvRow> iterator = csvMapper
                        .readerFor(TransactionCsvRow.class)
                        .with(schema)
                        .readValues(is);

                List<RawTransaction> transactions = iterator.readAll().stream()
                        .map(this::mapAndSaveTransaction)
                        .filter(tran -> tran != null)
                        .collect(Collectors.toList());

                if (!transactions.isEmpty()) {
                    log.info("Successfully loaded {} transactions from CSV", transactions.size());
                } else {
                    log.info("No transactions found in {}", transactionsFile);
                }
            }
        } catch (Exception e) {
            log.error("Failed to load transactions from '{}': {}", transactionsFile, e.getMessage());
            throw new RuntimeException("Failed to load transactions", e);
        }
    }

    private RawTransaction mapAndSaveTransaction(TransactionCsvRow row) {
        try {
            String customerId = row.getCustomerId().trim();
            String description = row.getDescription().trim();
            BigDecimal amount = row.getAmount();
            LocalDateTime timestamp = row.getTimestamp();
            String source = row.getSource().trim();

            // Handle null or invalid values with defaults
            if (amount == null) {
                log.warn("Invalid amount for transaction, defaulting to ZERO");
                amount = BigDecimal.ZERO;
            }

            if (timestamp == null) {
                log.warn("Invalid timestamp for transaction, defaulting to now");
                timestamp = LocalDateTime.now();
            }

            // Create RawTransaction and save it
            var rawTransaction = new RawTransaction(customerId, description, amount, timestamp, source);
            rawTransactionRepository.save(rawTransaction);
            log.debug("Saved transaction: {} - {}", customerId, description);

            return rawTransaction;

        } catch (Exception e) {
            log.error("Error mapping transaction row: {}", row, e);
            return null;
        }
    }

    /**
     * DTO for mapping CSV rows to transaction objects
     * Expected CSV header: customerId,description,amount,timestamp,source
     */
    @Data
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class TransactionCsvRow {
        @com.fasterxml.jackson.annotation.JsonProperty("customerId")
        private String customerId;
        @com.fasterxml.jackson.annotation.JsonProperty("description")
        private String description;
        @com.fasterxml.jackson.annotation.JsonProperty("amount")
        private BigDecimal amount;
        @com.fasterxml.jackson.annotation.JsonProperty("timestamp")
        private LocalDateTime timestamp;
        @com.fasterxml.jackson.annotation.JsonProperty("source")
        private String source;
    }
}