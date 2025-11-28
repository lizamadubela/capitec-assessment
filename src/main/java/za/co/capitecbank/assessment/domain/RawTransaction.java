package za.co.capitecbank.assessment.domain;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class RawTransaction {
    private final String customerId;
    private final String description;
    private final BigDecimal amount;
    private final LocalDateTime timestamp;


    public RawTransaction(String customerId, String description, BigDecimal amount, LocalDateTime timestamp) {
        this.customerId = customerId;
        this.description = description;
        this.amount = amount;
        this.timestamp = timestamp;
    }
}
