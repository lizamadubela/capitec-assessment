package za.co.capitecbank.assessment.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;


public class RawTransaction {
    private final String accountId;
    private final String description;
    private final BigDecimal amount;
    private final LocalDateTime timestamp;


    public RawTransaction(String accountId, String description, BigDecimal amount, LocalDateTime timestamp) {
        this.accountId = accountId;
        this.description = description;
        this.amount = amount;
        this.timestamp = timestamp;
    }


    public String getAccountId() { return accountId; }
    public String getDescription() { return description; }
    public BigDecimal getAmount() { return amount; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
