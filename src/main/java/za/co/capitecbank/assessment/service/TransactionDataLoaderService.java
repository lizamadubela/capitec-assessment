package za.co.capitecbank.assessment.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import za.co.capitecbank.assessment.domain.RawTransaction;
import za.co.capitecbank.assessment.domain.entity.Transaction;
import za.co.capitecbank.assessment.repository.TransactionRepository;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
@Order(2)
public class TransactionDataLoaderService {

    private final TransactionRepository transactionRepository;
    private final ResourceLoader resourceLoader;
    private final TxCategorizationEngine categorizationEngine;

    @Value("${app.data-file:classpath:transactions.csv}")
    private String transactionsFile;

    public TransactionDataLoaderService(TransactionRepository transactionRepository,
                                 ResourceLoader resourceLoader,
                                 TxCategorizationEngine categorizationEngine) {
        this.transactionRepository = transactionRepository;
        this.resourceLoader = resourceLoader;
        this.categorizationEngine = categorizationEngine;
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

            try (var is = resource.getInputStream();
                 var reader = new BufferedReader(new InputStreamReader(is))) {

                // Expected CSV header: customerId,description,amount,timestamp,source
                var transactions = reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty())
                        .filter(line -> !line.startsWith("#"))
                        .skip(1) // skip header
                        .map(this::parseCsvTxLineAndSave)
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

    private Transaction parseCsvTxLineAndSave(String line) {
        try {
            // Parse CSV line (handles quotes and commas within quotes)
            String[] parts = parseCsvLine(line);

            if (parts.length < 5) {
                log.warn("Invalid CSV line (expected 5 columns): {}", line);
                return null;
            }

            String customerId = parts[0].trim();
            String description = parts[1].trim();
            String amountStr = parts[2].trim();
            String timestampStr = parts[3].trim();
            String source = parts[4].trim();

            BigDecimal amount = BigDecimal.ZERO;
            try {
                amount = new BigDecimal(amountStr);
            } catch (Exception e) {
                log.warn("Invalid amount '{}' for line, defaulting to ZERO", amountStr);
            }

            LocalDateTime timestamp = LocalDateTime.now();
            try {
                timestamp = LocalDateTime.parse(timestampStr);
            } catch (DateTimeParseException e) {
                log.warn("Invalid timestamp '{}' for line, defaulting to now", timestampStr);
            }

            // Create RawTransaction, categorize, and save
            var rawTransaction = new RawTransaction(customerId, description, amount, timestamp);
            var categorizedTransaction = categorizationEngine.categorize(rawTransaction, source);

            Transaction entity = new Transaction(
                    categorizedTransaction.getCustomerId(),
                    categorizedTransaction.getAmount(),
                    categorizedTransaction.getTimestamp(),
                    categorizedTransaction.getDescription(),
                    categorizedTransaction.getCategory(),
                    categorizedTransaction.getSource()
            );

            transactionRepository.save(entity);
            log.debug("Saved transaction: {} - {}", customerId, description);

            return entity;

        } catch (Exception e) {
            log.error("Error parsing CSV line: {}", line, e);
            return null;
        }
    }

    /**
     * Simple CSV parser that handles quoted fields
     */
    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(currentField.toString());
                currentField = new StringBuilder();
            } else {
                currentField.append(c);
            }
        }

        result.add(currentField.toString());
        return result.toArray(new String[0]);
    }
}
