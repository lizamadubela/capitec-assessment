package za.co.capitecbank.assessment.transactions.source.impl;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import za.co.capitecbank.assessment.domain.entity.RawTransaction;
import za.co.capitecbank.assessment.transactions.source.TransactionSource;


import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class RandomTransactionsGenerator implements TransactionSource {

    private List<String> transactionDescriptions = new ArrayList<>();
    private List<String> transactionSources = new ArrayList<>();

    private final int NUMBER_OF_TRANSACTIONS = 30;


    @Override
    public List<RawTransaction> fetchTransactions() {
        return generateTransactions(NUMBER_OF_TRANSACTIONS);
    }

    @PostConstruct
    public void init() throws IOException, CsvException {
        loadTransactionDescriptions();
        loadTransactionSources();
    }

    private void loadTransactionDescriptions() throws IOException, CsvException {
        ClassPathResource resource = new ClassPathResource("categories.csv");
        try (CSVReader reader = new CSVReader(new InputStreamReader(resource.getInputStream()))) {
            List<String[]> rows = reader.readAll();

            // Skip header row
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                if (row.length >= 4) {
                    String keywordsColumn = row[3];
                    String[] keywords = keywordsColumn.split(",");
                    for (String keyword : keywords) {
                        String trimmed = keyword.trim();
                        if (!trimmed.isEmpty()) {
                            transactionDescriptions.add(trimmed);
                        }
                    }
                }
            }
        }

        if (transactionDescriptions.isEmpty()) {
            throw new IllegalStateException("No transaction descriptions loaded from CSV");
        }
    }

    private void loadTransactionSources() throws IOException {
        ClassPathResource resource = new ClassPathResource("transaction-sources.txt");
        try (Scanner scanner = new Scanner(new InputStreamReader(resource.getInputStream()))) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    transactionSources.add(line);
                }
            }
        }

        if (transactionSources.isEmpty()) {
            throw new IllegalStateException("No transaction sources loaded from file");
        }
    }

    public RawTransaction generateRandomTransaction() {
        RawTransaction transaction = new RawTransaction();

        // Random customer ID in format CUST-Number
        transaction.setCustomerId("CUST-" + ThreadLocalRandom.current().nextInt(1, 10));

        // Random description
        String description = transactionDescriptions.get(
                ThreadLocalRandom.current().nextInt(transactionDescriptions.size())
        );
        transaction.setDescription(description);

        // Random amount (mix of positive and negative)
        BigDecimal amount = generateAmount();
        transaction.setAmount(amount);

        // Random source
        transaction.setSource(transactionSources.get(
                ThreadLocalRandom.current().nextInt(transactionSources.size())
        ));

        // Random date within last 30 days
        LocalDateTime dateTime = LocalDateTime.now()
                .minusDays(ThreadLocalRandom.current().nextInt(30))
                .minusHours(ThreadLocalRandom.current().nextInt(24))
                .minusMinutes(ThreadLocalRandom.current().nextInt(60));
        transaction.setTimestamp(dateTime.truncatedTo(java.time.temporal.ChronoUnit.SECONDS));

        return transaction;
    }


    public List<RawTransaction> generateTransactions(int count) {
        List<RawTransaction> transactions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            transactions.add(generateRandomTransaction());
        }
        // Sort by date descending
        transactions.sort(Comparator.comparing(RawTransaction::getTimestamp).reversed());
        return transactions;
    }

    private BigDecimal generateAmount() {
        // 70% chance of expense (negative), 30% chance of income (positive)
        boolean isExpense = ThreadLocalRandom.current().nextDouble() < 0.7;

        double amount;
        if (isExpense) {
            // Expenses: R10 to R5,000
            amount = ThreadLocalRandom.current().nextDouble(10.0, 5000.0) * -1;
        } else {
            // Income: R100 to R50,000
            amount = ThreadLocalRandom.current().nextDouble(100.0, 50000.0);
        }

        return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);
    }
}
