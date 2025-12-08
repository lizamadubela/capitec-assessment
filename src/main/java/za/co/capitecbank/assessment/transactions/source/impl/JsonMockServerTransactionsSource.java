package za.co.capitecbank.assessment.transactions.source.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import za.co.capitecbank.assessment.domain.entity.RawTransaction;
import za.co.capitecbank.assessment.transactions.source.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Component
public class JsonMockServerTransactionsSource implements TransactionSource {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public JsonMockServerTransactionsSource(RestTemplate restTemplate,
                                            @Value("${app.json-server.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    @Override
    public List<RawTransaction> fetchTransactions() {
        List<RawTransaction> rawTransactions = new ArrayList<>();

        try {
            String url = baseUrl + "/api/transactions";

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("transactions")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> transactions = (List<Map<String, Object>>) body.get("transactions");

                if (transactions != null) {
                    for (Map<String, Object> txn : transactions) {
                        try {
                            rawTransactions.add(mapToRaw(txn));
                        } catch (Exception e) {
                            System.err.println("Error mapping transaction: " + e.getMessage());
                        }
                    }
                }
            }

        } catch (Exception ex) {
            System.err.println("Error fetching transactions from MockServer: " + ex.getMessage());
            ex.printStackTrace();
        }

        return rawTransactions;
    }

    private RawTransaction mapToRaw(Map<String, Object> m) {
        String customerId = String.valueOf(m.getOrDefault("customerId", ""));
        String description = String.valueOf(m.getOrDefault("description", ""));
        String source = String.valueOf(m.getOrDefault("source", "MOCK SERVER"));

        BigDecimal amount = BigDecimal.ZERO;
        Object amountObj = m.get("amount");
        if (amountObj != null) {
            try {
                amount = new BigDecimal(amountObj.toString());
            } catch (NumberFormatException e) {
                System.err.println("Invalid amount format: " + amountObj);
                amount = BigDecimal.ZERO;
            }
        }

        LocalDateTime timestamp = LocalDateTime.now();
        Object timestampObj = m.get("timestamp");
        if (timestampObj != null) {
            try {
                String ts = timestampObj.toString();
                // Try ISO format first (2025-12-06T10:15:20)
                timestamp = LocalDateTime.parse(ts);
            } catch (DateTimeParseException e1) {
                try {
                    // Try with custom formatter if ISO fails
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    timestamp = LocalDateTime.parse(timestampObj.toString(), formatter);
                } catch (Exception e2) {
                    System.err.println("Invalid timestamp format: " + timestampObj + ", using current time");
                    timestamp = LocalDateTime.now();
                }
            }
        }

        return new RawTransaction(customerId, description, amount, timestamp, source);
    }
}