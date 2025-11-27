package za.co.capitecbank.assessment.domain.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(name = "transactions")
public class TransactionEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(nullable = false)
    private String accountId;


    @Column(nullable = false)
    private BigDecimal amount;


    @Column(nullable = false)
    private LocalDateTime timestamp;


    private String description;
    private String category;
    private String source;


    public TransactionEntity() {}


    public TransactionEntity(String accountId, BigDecimal amount, LocalDateTime timestamp, String description, String category, String source) {
        this.accountId = accountId;
        this.amount = amount;
        this.timestamp = timestamp;
        this.description = description;
        this.category = category;
        this.source = source;
    }


    public Long getId() { return id; }
    public String getAccountId() { return accountId; }
    public BigDecimal getAmount() { return amount; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public String getSource() { return source; }


    public void setId(Long id) { this.id = id; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public void setDescription(String description) { this.description = description; }
    public void setCategory(String category) { this.category = category; }
    public void setSource(String source) { this.source = source; }
}
