package za.co.capitecbank.assessment.transactions.source.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import za.co.capitecbank.assessment.domain.entity.RawTransaction;
import za.co.capitecbank.assessment.transactions.source.TransactionSource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class FlatFileBasedTransactionsSource implements TransactionSource {

    private final Resource flatFile;
    private static final DateTimeFormatter ISO_TS = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public FlatFileBasedTransactionsSource(@Value("${app.flat-file}") Resource flatFile) {
        this.flatFile = flatFile;
    }

    @Override
    public List<RawTransaction> fetchTransactions() {
        List<RawTransaction> list = new ArrayList<>();

        try (InputStream is = flatFile.getInputStream();
             BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            String line;
            int lineNo = 0;

            while ((line = br.readLine()) != null) {
                lineNo++;
                if (line.trim().isEmpty()) continue;

                try {
                    int pos = 0;

                    // Skip transactionId (11 chars)
                    pos += 11;

                    // customerId (10 chars) - EXTRACT
                    String customerId = sub(line, pos, pos += 10).trim();

                    // description (33 chars) - EXTRACT
                    String description = sub(line, pos, pos += 33).trim();

                    // Skip merchant (19 chars)
                    pos += 19;

                    // Skip reference (16 chars)
                    pos += 16;

                    // Skip type (6 chars)
                    pos += 6;

                    // amount (9 chars - format: 000245.90) - EXTRACT
                    String amountStr = sub(line, pos, pos += 9).trim();

                    // Skip currency (3 chars)
                    pos += 3;

                    // timestamp (19 chars - format: 2025-10-12T14:21:10) - EXTRACT
                    String timestampStr = sub(line, pos, pos += 19).trim();

                    // source (originating channel) - EXTRACT
                    String source = sub(line, pos, pos += 9).trim();

                    // Parse amount
                    BigDecimal amount = parseAmount(amountStr);

                    // Parse timestamp
                    LocalDateTime timestamp = parseTimestamp(timestampStr);

                    // Create RawTransaction with only the 5 required fields
                    RawTransaction tx = new RawTransaction(
                            customerId,
                            description,
                            amount,
                            timestamp,
                            source
                    );

                    list.add(tx);

                } catch (Exception ex) {
                    System.err.println("FlatFileBasedTransactionsSource: failed parsing line " + lineNo + " -> " + ex.getMessage());
                    System.err.println("Line content: " + line);
                    ex.printStackTrace();
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to read flat file: " + flatFile.getFilename(), e);
        }

        return list;
    }

    private static String sub(String s, int start, int end) {
        if (start >= s.length()) return "";
        return s.substring(start, Math.min(end, s.length()));
    }

    private BigDecimal parseAmount(String raw) {
        try {
            String cleaned = raw.replaceAll("\\s", "");
            return cleaned.isEmpty() ? BigDecimal.ZERO : new BigDecimal(cleaned);
        } catch (Exception e) {
            System.err.println("Failed to parse amount: '" + raw + "'");
            return BigDecimal.ZERO;
        }
    }

    private LocalDateTime parseTimestamp(String raw) {
        try {
            return LocalDateTime.parse(raw, ISO_TS);
        } catch (Exception e) {
            try {
                DateTimeFormatter alt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                return LocalDateTime.parse(raw, alt);
            } catch (Exception e2) {
                System.err.println("Failed to parse timestamp: '" + raw + "'");
                return LocalDateTime.now();
            }
        }
    }
}