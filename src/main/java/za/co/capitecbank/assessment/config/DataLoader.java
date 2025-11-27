package za.co.capitecbank.assessment.config;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import za.co.capitecbank.assessment.domain.RawTransaction;
import za.co.capitecbank.assessment.domain.entity.TransactionEntity;
import za.co.capitecbank.assessment.repository.TransactionRepository;
import za.co.capitecbank.assessment.service.TxCategorizationEngine;
import za.co.capitecbank.assessment.tx_source.TransactionSource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class DataLoader {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    @Value("${app.data-file:classpath:transactions.csv}")
    private String dataFile;

    @Bean
    CommandLineRunner loadInitialData(ResourceLoader resourceLoader,
                                      List<TransactionSource> providers,
                                      TxCategorizationEngine engine,
                                      TransactionRepository repository) {
        return args -> {
            // 1) Try to import CSV from configured resource
            try {
                Resource resource = resourceLoader.getResource(dataFile);
                if (resource.exists()) {
                    try (var is = resource.getInputStream();
                         var reader = new BufferedReader(new InputStreamReader(is))) {

                        // expected CSV header: accountId,description,amount,timestamp,source
                        var entities = reader.lines()
                                .map(String::trim)
                                .filter(line -> !line.isEmpty())
                                .filter(line -> !line.startsWith("#"))
                                .skip(1) // skip header
                                .map(line -> parseAndMapToEntity(line, engine))
                                .collect(Collectors.toList());

                        if (!entities.isEmpty()) {
                            repository.saveAll(entities);
                            log.info("Loaded {} transactions from {}", entities.size(), dataFile);
                        } else {
                            log.info("No entries found in {}", dataFile);
                        }
                    }
                    return;
                } else {
                    log.info("Configured data-file not found: {} — falling back to providers", dataFile);
                }
            } catch (Exception e) {
                log.error("Failed to load external data-file '{}': {}", dataFile, e.getMessage());
            }

            // 2) Fallback: preload from sources
            String demoCustomer = "CUST-1";
            providers.forEach(p -> {
                var raws = p.fetchTransactions(demoCustomer);
                var entities = raws.stream()
                        .map(r -> engine.categorize(r, p.getClass().getSimpleName()))
                        .map(t -> new TransactionEntity(t.getAccountId(), t.getAmount(),
                                t.getTimestamp(), t.getDescription(), t.getCategory(), t.getSource()))
                        .collect(Collectors.toList());
                repository.saveAll(entities);
            });
            log.info("Preloaded provider demo data for {}", demoCustomer);
        };
    }

    private TransactionEntity parseAndMapToEntity(String line, TxCategorizationEngine engine) {
        var cols = line.split(",", -1);
        var accountId = cols.length > 0 ? cols[0].trim() : "";
        var description = cols.length > 1 ? cols[1].trim() : "";
        var amountStr = cols.length > 2 ? cols[2].trim() : "0";
        var tsStr = cols.length > 3 ? cols[3].trim() : "";
        var source = cols.length > 4 ? cols[4].trim() : "external";

        BigDecimal amount = BigDecimal.ZERO;
        try {
            amount = new BigDecimal(amountStr);
        } catch (Exception e) {
            log.warn("Invalid amount '{}' for line, defaulting to ZERO", amountStr);
        }

        LocalDateTime ts = LocalDateTime.now();
        try {
            ts = LocalDateTime.parse(tsStr);
        } catch (DateTimeParseException e) {
            log.warn("Invalid timestamp '{}' for line, defaulting to now", tsStr);
        }

        // Direct mapping: create RawTransaction, categorize, map to entity
        var raw = new RawTransaction(accountId, description, amount, ts);
        var txn = engine.categorize(raw, source);

        return new TransactionEntity(
                txn.getAccountId(),
                txn.getAmount(),
                txn.getTimestamp(),
                txn.getDescription(),
                txn.getCategory(),
                txn.getSource()
        );
    }
}
