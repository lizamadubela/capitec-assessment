package za.co.capitecbank.assessment.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "raw_transaction", schema = "data_aggregation")
@Getter
@Setter
@NoArgsConstructor
public class RawTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "source", nullable = false)
    private String source;

    public RawTransaction(String customerId, String description, BigDecimal amount, LocalDateTime timestamp, String source) {
        this.customerId = customerId;
        this.description = description;
        this.amount = amount;
        this.timestamp = timestamp;
        this.source = source;
    }
}