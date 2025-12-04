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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        String url = baseUrl + "/transactions";

        ResponseEntity<List<Map<String, Object>>> resp = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        );

        return resp.getBody().stream()
                .map(this::mapToRaw)
                .collect(Collectors.toList());
    }

    private RawTransaction mapToRaw(Map<String, Object> m) {
        String customerId = (String) m.getOrDefault("customerId", "");
        String description = (String) m.getOrDefault("description", "");
        String source = (String) m.getOrDefault("source", "");
        BigDecimal amount = BigDecimal.ZERO;
        Object amountObj = m.get("amount");
        if (amountObj != null) {
            amount = new BigDecimal(amountObj.toString());
        }
        String ts = (String) m.getOrDefault("timestamp", "");
        LocalDateTime timestamp = LocalDateTime.now();
        try {
            timestamp = LocalDateTime.parse(ts);
        } catch (Exception ignored) {
        }

        return new RawTransaction(customerId, description, amount, timestamp, source);
    }
}
