package za.co.capitecbank.assessment.domain.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(name = "transactions")
public class TransactionEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;


    @Column(nullable = false)
    private String customerId;


    @Column(nullable = false)
    private BigDecimal amount;


    @Column(nullable = false)
    private LocalDateTime timestamp;


    private String description;
    private String category;
    private String source;


    public TransactionEntity() {}


    public TransactionEntity(String customerId, BigDecimal amount, LocalDateTime timestamp, String description, String category, String source) {
        this.customerId = customerId;
        this.amount = amount;
        this.timestamp = timestamp;
        this.description = description;
        this.category = category;
        this.source = source;
    }


    public Long getTransactionId() { return transactionId; }
    public String getCustomerId() { return customerId; }
    public BigDecimal getAmount() { return amount; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public String getSource() { return source; }


    public void setTransactionId(Long id) { this.transactionId = id; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public void setDescription(String description) { this.description = description; }
    public void setCategory(String category) { this.category = category; }
    public void setSource(String source) { this.source = source; }
}
