package za.co.capitecbank.assessment.domain;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class Transaction {

    private final String transactionId;
    private final String customerId;
    private final BigDecimal amount;
    private final LocalDateTime timestamp;
    private final String description;
    private final String category;
    private final String source;


    public Transaction(String transactionId, String customerId, BigDecimal amount, LocalDateTime timestamp, String description, String category, String source) {
        this.transactionId = transactionId;
        this.customerId = customerId;
        this.amount = amount;
        this.timestamp = timestamp;
        this.description = description;
        this.category = category;
        this.source = source;
    }
}