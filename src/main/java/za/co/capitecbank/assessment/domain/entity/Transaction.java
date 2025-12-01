package za.co.capitecbank.assessment.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Setter
@Getter
@Table(name = "transaction", schema = "data_aggregation")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String customerId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    private String description;

    @Transient  // field won't be persisted to database
    private String category;

    private String source;

    public Transaction() {}

    public Transaction(String customerId, BigDecimal amount, LocalDateTime timestamp, String description, String category, String source) {
        this.customerId = customerId;
        this.amount = amount;
        this.timestamp = timestamp;
        this.description = description;
        this.category = category;
        this.source = source;
    }
}