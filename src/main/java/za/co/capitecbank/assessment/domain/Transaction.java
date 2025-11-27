package za.co.capitecbank.assessment.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;


public class Transaction {
    private final String accountId;
    private final BigDecimal amount;
    private final LocalDateTime timestamp;
    private final String description;
    private final String category;
    private final String source;


    public Transaction(String accountId, BigDecimal amount, LocalDateTime timestamp, String description, String category, String source) {
        this.accountId = accountId;
        this.amount = amount;
        this.timestamp = timestamp;
        this.description = description;
        this.category = category;
        this.source = source;
    }


    public String getAccountId() { return accountId; }
    public BigDecimal getAmount() { return amount; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public String getSource() { return source; }
}
