package za.co.capitecbank.assessment.tx_source.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import za.co.capitecbank.assessment.domain.entity.RawTransaction;
import za.co.capitecbank.assessment.tx_source.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Component
public class MultiSource implements TransactionSource {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public MultiSource(RestTemplate restTemplate,
                       @Value("${app.json-server.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    @Override
    public List<RawTransaction> fetchTransactions() {
        List<RawTransaction> rawTransactions = new ArrayList<>();

        int index = 0;
        int consecutiveFailures = 0;
        int maxConsecutiveFailures = 3; // Stop after 3 consecutive 404s

        while (consecutiveFailures < maxConsecutiveFailures) {
            String url = baseUrl + "/" + index;

            try {
                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<Map<String, Object>>() {}
                );

                Map<String, Object> body = response.getBody();
                if (body != null && !body.isEmpty()) {
                    rawTransactions.add(mapToRaw(body));
                    consecutiveFailures = 0; // Reset on success
                }

                index++;

            } catch (Exception ex) {
                // If we get a 404 or any error, increment consecutive failures
                consecutiveFailures++;
                index++;

                // Only log if it's not a 404 (which is expected when we reach the end)
                if (!ex.getMessage().contains("404")) {
                    System.err.println("Error fetching transaction from endpoint " + (index - 1) + ": " + ex.getMessage());
                }
            }
        }

        return rawTransactions;
    }

    private RawTransaction mapToRaw(Map<String, Object> m) {
        String customerId = String.valueOf(m.getOrDefault("customerId", ""));
        String description = String.valueOf(m.getOrDefault("description", ""));
        String source = String.valueOf(m.getOrDefault("source", "MULTI_SOURCE"));

        BigDecimal amount = BigDecimal.ZERO;
        Object amountObj = m.get("amount");
        if (amountObj != null) {
            try {
                amount = new BigDecimal(amountObj.toString());
            } catch (NumberFormatException e) {
                // Log warning if needed
                amount = BigDecimal.ZERO;
            }
        }

        LocalDateTime timestamp = LocalDateTime.now();
        Object timestampObj = m.get("timestamp");
        if (timestampObj != null) {
            try {
                String ts = timestampObj.toString();
                // Try ISO format first
                timestamp = LocalDateTime.parse(ts);
            } catch (DateTimeParseException e1) {
                try {
                    // Try with custom formatter if ISO fails
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    timestamp = LocalDateTime.parse(timestampObj.toString(), formatter);
                } catch (Exception e2) {
                    // Fall back to current time
                    timestamp = LocalDateTime.now();
                }
            }
        }

        return new RawTransaction(customerId, description, amount, timestamp, source);
    }
}